package com.chit.app.domain.session.application.service

import com.chit.app.domain.live.application.LiveStreamCommandService
import com.chit.app.domain.member.application.MemberQueryService
import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.domain.exception.DuplicateContentsSessionException
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.domain.sse.infrastructure.SseAdapter
import com.chit.app.domain.sse.infrastructure.SseEmitterManager
import com.chit.app.domain.sse.infrastructure.SseEventType
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SessionCommandService(
        private val sseAdapter: SseAdapter,
        private val sseEmitterManager: SseEmitterManager,
        
        private val memberQueryService: MemberQueryService,
        private val sessionQueryService: SessionQueryService,
        private val liveStreamCommandService: LiveStreamCommandService,
        
        private val sessionRepository: SessionRepository,
) {
    
    private val log = logger<SessionCommandService>()
    
    fun createContentsSession(
            streamerId: Long,
            gameParticipationCode: String?,
            maxGroupParticipants: Int
    ): ContentsSessionResponseDto {
        log.info(
            "[요청] 콘텐츠 세션 생성 요청 (streamerId={}, gameParticipationCode={}, maxGroupParticipants={})",
            streamerId,
            gameParticipationCode,
            maxGroupParticipants
        )
        val liveStream = liveStreamCommandService.saveOrUpdate(streamerId)
        val liveId = liveStream.liveId!!
        
        if (!sessionRepository.notExistsOpenContentsSession(liveId)) {
            log.info("[실패] 콘텐츠 세션 생성 불가 - 이미 열린 세션 존재 (liveId={})", liveId)
            throw DuplicateContentsSessionException()
        }
        
        val createdContentsSession = ContentsSession.create(liveId, streamerId, maxGroupParticipants, gameParticipationCode)
        val contentsSession = sessionRepository.save(createdContentsSession)
        log.info("[성공] 콘텐츠 세션 생성 완료 (sessionCode={}, streamerId={})", contentsSession.sessionCode, streamerId)
        
        return contentsSession.toResponseDto()
    }
    
    fun modifySessionSettings(
            streamerId: Long,
            maxGroupParticipants: Int,
            gameParticipationCode: String?
    ): ContentsSessionResponseDto {
        log.info(
            "[요청] 세션 설정 변경 요청 (streamerId={}, maxGroupParticipants={}, gameParticipationCode={})",
            streamerId,
            maxGroupParticipants,
            gameParticipationCode
        )
        val contentsSession = sessionQueryService.getOpenContentsSession(streamerId = streamerId)
        log.info("[진행] 세션 설정 변경 대상 세션 조회 완료 (sessionCode={})", contentsSession.sessionCode)
        val updatedContentsSession = contentsSession.updateGameSettings(gameParticipationCode, maxGroupParticipants).toResponseDto()
        
        sseAdapter.emitStreamerSessionUpdateEventAsync(streamerId, contentsSession, updatedContentsSession)
        log.info("[성공] 세션 설정 변경 완료 (sessionCode={})", updatedContentsSession.sessionCode)
        return updatedContentsSession
    }
    
    fun closeContentsSession(streamerId: Long) {
        log.info("[요청] 시참 세션 닫기 요청 (streamerId={})", streamerId)
        val session = sessionQueryService.getOpenContentsSession(streamerId = streamerId).apply { close() }
        val sessionCode = session.sessionCode
        log.info("[진행] 세션 조회 및 닫힘 처리 완료 (sessionCode={})", sessionCode)
        
        ParticipantOrderManager.removeParticipantOrderQueue(sessionCode)
        sessionRepository.setAllParticipantsToLeft(sessionCode)
        log.info("[진행] 모든 참여자 상태 '퇴장' 처리 (sessionCode={})", sessionCode)
        
        sseAdapter.broadcastEvent(sessionCode, SseEventType.CLOSED_SESSION, null)
        sseEmitterManager.unsubscribeAll(sessionCode)
        log.info("[성공] 시참 세션 종료 브로드캐스트 및 SSE 구독 해제 완료 (sessionCode={})", sessionCode)
    }
    
    fun switchFixedPickStatus(streamerId: Long, viewerId: Long?) {
        log.info("[요청] 고정픽 상태 변경 요청 (streamerId={}, viewerId={})", streamerId, viewerId)
        val validViewerId = requireNotNull(viewerId) { "유효하지 않은 참여자 정보입니다." }
        
        val contentsSession = sessionQueryService.getOpenContentsSession(streamerId = streamerId)
        val chzzkNickname = memberQueryService.getMember(memberId = streamerId).channelName
        
        val participant = sessionQueryService.getParticipant(viewerId = validViewerId, sessionId = contentsSession.id!!)
        log.debug("[진행] 고정픽 상태 변경 전 (participantId={}, fixedPick={})", participant.id, participant.fixedPick)
        participant.toggleFixedPick()
        log.debug("[진행] 고정픽 상태 변경 후 (participantId={}, fixedPick={})", participant.id, participant.fixedPick)
        
        log.debug("[진행] 참여자 순서 큐 갱신 요청 (sessionCode={}, participantId={})", contentsSession.sessionCode, participant.id)
        ParticipantOrderManager.addOrUpdateParticipantOrder(contentsSession.sessionCode, participant, validViewerId, chzzkNickname)
        
        log.debug("[알림] 고정픽 SSE 이벤트 발송 (participantId={})", participant.id)
        sseAdapter.emitParticipantFixedEventAsync(participant)
        log.info("[성공] 고정픽 상태 변경 완료 (streamerId={}, viewerId={}, 닉네임={})", streamerId, validViewerId, chzzkNickname)
    }
    
    fun proceedToNextGroup(streamerId: Long) {
        val session = sessionQueryService.getOpenContentsSession(streamerId = streamerId)
        log.info("[요청] 다음 그룹 진행 요청 (sessionCode={})", session.sessionCode)
        val participants = sessionRepository.findFirstPartyParticipants(session.id!!, session.maxGroupParticipants)
        
        log.info("[진행] 세션 참가자 조회 완료 (sessionCode={}, count={})", session.sessionCode, participants.size)
        participants.forEach { participant -> participant.incrementSessionRound() }
        
        ParticipantOrderManager.rotateFirstNOrdersToNextCycle(session)
        sseAdapter.notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, session)
        log.info("[성공] 그룹 회전 처리 완료 (sessionCode={})", session.sessionCode)
    }
    
    fun addParticipantToSession(participant: SessionParticipant) =
            sessionRepository.addParticipant(participant)
    
    private fun ContentsSession.toResponseDto(): ContentsSessionResponseDto {
        return ContentsSessionResponseDto(
            sessionCode = sessionCode,
            gameParticipationCode = gameParticipationCode,
            maxGroupParticipants = maxGroupParticipants,
            currentParticipants = currentParticipants
        )
    }
}