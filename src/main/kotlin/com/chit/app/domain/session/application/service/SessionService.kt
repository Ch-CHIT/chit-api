package com.chit.app.domain.session.application.service

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.member.application.MemberService
import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.domain.sse.infrastructure.SseAdapter
import com.chit.app.domain.sse.infrastructure.SseEmitterManager
import com.chit.app.domain.sse.infrastructure.SseEventType
import com.chit.app.global.common.logging.logger
import com.chit.app.global.common.response.SuccessResponse.PagedResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SessionService(
        private val sseAdapter: SseAdapter,
        private val sseEmitterManager: SseEmitterManager,
        
        private val memberService: MemberService,
        private val sessionRepository: SessionRepository,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    private val log = logger<SessionService>()
    
    @Transactional
    fun createContentsSession(
            streamerId: Long,
            gameParticipationCode: String?,
            maxGroupParticipants: Int
    ): ContentsSessionResponseDto {
        // 열린 라이브 스트림을 조회 (스트리머가 현재 방송 중인지 확인)
        val openLiveStream = getOpenLiveStreamByStreamerId(streamerId)
        val liveId = openLiveStream.liveId
        // 이미 진행 중인 컨텐츠 세션이 없음을 검증
        check(sessionRepository.notExistsOpenContentsSession(liveId!!)) { "이미 진행 중인 컨텐츠 세션이 존재합니다. 중복 생성을 할 수 없습니다." }
        
        // 새로운 컨텐츠 세션 생성 및 저장
        val createdContentsSession = ContentsSession.create(liveId, streamerId, maxGroupParticipants, gameParticipationCode)
        val contentsSession = sessionRepository.save(createdContentsSession)
        
        // 세션 생성 완료 로그 기록
        log.info("컨텐츠 세션 생성 완료: 세션코드: ${contentsSession.sessionCode}, 스트리머ID: $streamerId")
        return contentsSession.toResponseDto()
    }
    
    @Transactional(readOnly = true)
    fun getOpeningContentsSession(streamerId: Long, pageable: Pageable): ContentsSessionResponseDto {
        // 현재 진행중인 시청자 참여 세션 조회
        val contentsSession = getOpenContentsSessionByStreamerId(streamerId)
        
        // 세션에 속한 참여자들을 페이징 처리하여 조회
        val participants = sessionRepository.findPagedParticipantsBySessionCode(contentsSession.sessionCode, pageable)
        return ContentsSessionResponseDto(
            sessionCode = contentsSession.sessionCode,
            gameParticipationCode = contentsSession.gameParticipationCode,
            maxGroupParticipants = contentsSession.maxGroupParticipants,
            currentParticipants = contentsSession.currentParticipants,
            participants = PagedResponse.from(participants)
        )
    }
    
    @Transactional
    fun modifySessionSettings(
            streamerId: Long,
            maxGroupParticipants: Int,
            gameParticipationCode: String?
    ): ContentsSessionResponseDto {
        // 현재 열린 세션 조회 & 세션 설정 업데이트
        val contentsSession = getOpenContentsSessionByStreamerId(streamerId)
        val updatedContentsSession = contentsSession.updateGameSettings(gameParticipationCode, maxGroupParticipants).toResponseDto()
        
        // SSE 업데이트 이벤트 전송
        sseAdapter.emitStreamerSessionUpdateEventAsync(streamerId, contentsSession, updatedContentsSession)
        return updatedContentsSession
    }
    
    @Transactional
    fun closeContentsSession(streamerId: Long) {
        // 세션 조회 및 종료
        val session = getOpenContentsSessionByStreamerId(streamerId).apply { close() }
        val sessionCode = session.sessionCode
        
        // 참여자 순서 클리어 및 모든 참가자 `LEFT` 로 설정
        ParticipantOrderManager.removeParticipantOrderQueue(sessionCode)
        sessionRepository.setAllParticipantsToLeft(sessionCode)
        
        // 세션 종료 이벤트 방송
        sseAdapter.broadcastEvent(sessionCode, SseEventType.CLOSED_SESSION, null)
        // 세션에 대한 모든 SSE 구독 해제
        sseEmitterManager.unsubscribeAll(sessionCode)
    }
    
    @Transactional
    fun switchFixedPickStatus(streamerId: Long, viewerId: Long?) {
        // viewerId는 필수 값이므로 null 체크 후, null이 아닐 경우에만 진행
        val validViewerId = requireNotNull(viewerId) { "유효하지 않은 참여자 정보입니다." }
        
        // 현재 열린 세션 & 참여자 치지직 닉네임 조회
        val session = getOpenContentsSessionByStreamerId(streamerId)
        val chzzkNickname = memberService.getChzzkNickname(validViewerId)
        
        // 참여자 정보 조회 후, 고정 상태 토글 및 순서 업데이트 처리
        val participant = getParticipantByViewerIdAndSessionId(validViewerId, session.id!!)
        participant.toggleFixedPick()
        ParticipantOrderManager.addOrUpdateParticipantOrder(session.sessionCode, participant, validViewerId, chzzkNickname)
        
        // 스트리머에게 고정 이벤트를 비동기적으로 전송
        sseAdapter.emitParticipantFixedEventAsync(participant)
    }
    
    @Transactional(readOnly = true)
    fun retrieveGameParticipationCode(sessionCode: String, viewerId: Long): ContentsSessionResponseDto {
        val gameParticipationCode = getGameParticipationCodeBySessionCodeAndViewerId(sessionCode, viewerId)
        return ContentsSessionResponseDto(gameParticipationCode = gameParticipationCode)
    }
    
    @Transactional
    fun proceedToNextGroup(streamerId: Long) {
        // 현재 진행중인 시청자 참여 세션 조회
        val session = getOpenContentsSessionByStreamerId(streamerId)
        
        // 첫 그룹 참여자들의 라운드 증가
        val participants = sessionRepository.findFirstPartyParticipants(session.id!!, session.maxGroupParticipants)
        participants.forEach { participant -> participant.incrementSessionRound() }
        
        // 순서 사이클 진행
        ParticipantOrderManager.rotateFirstNOrdersToNextCycle(session)
        
        // SSE 순서 업데이트 이벤트 전송
        sseAdapter.notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, session)
    }
    
    private fun getOpenContentsSessionByStreamerId(streamerId: Long): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(streamerId = streamerId)
                ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다. 다시 확인해 주세요.")
    }
    
    private fun getOpenLiveStreamByStreamerId(streamerId: Long): LiveStream {
        return liveStreamRepository.findOpenLiveStreamBy(streamerId = streamerId)
                ?: throw IllegalArgumentException("현재 진행중인 라이브 방송을 찾을 수 없습니다. 다시 확인해 주세요.")
    }
    
    private fun getParticipantByViewerIdAndSessionId(validViewerId: Long, sessionId: Long): SessionParticipant {
        return sessionRepository.findParticipantBy(validViewerId, sessionId = sessionId)
                ?: throw IllegalArgumentException("해당 세션에 유효한 참여자가 존재하지 않습니다.")
    }
    
    private fun getGameParticipationCodeBySessionCodeAndViewerId(sessionCode: String, viewerId: Long): String {
        return sessionRepository.findGameParticipationCodeBy(sessionCode, viewerId)
                ?: throw IllegalArgumentException("해당 세션에 참가자 정보가 존재하지 않습니다. 다시 확인해 주세요.")
    }
    
    private fun ContentsSession.toResponseDto(): ContentsSessionResponseDto {
        return ContentsSessionResponseDto(
            sessionCode = sessionCode,
            gameParticipationCode = gameParticipationCode,
            maxGroupParticipants = maxGroupParticipants,
            currentParticipants = currentParticipants
        )
    }
}