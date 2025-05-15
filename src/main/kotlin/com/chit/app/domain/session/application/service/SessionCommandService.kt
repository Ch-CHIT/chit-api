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
            "콘텐츠 세션 생성 요청: streamerId={}, gameParticipationCode={}, maxGroupParticipants={}",
            streamerId,
            gameParticipationCode,
            maxGroupParticipants
        )
        val liveStream = liveStreamCommandService.saveOrUpdate(streamerId)
        val liveId = liveStream.liveId!!
        
        if (!sessionRepository.notExistsOpenContentsSession(liveId)) {
            log.info("이미 열린 세션이 존재함: liveId={}", liveId)
            throw DuplicateContentsSessionException()
        }
        
        val createdContentsSession = ContentsSession.create(liveId, streamerId, maxGroupParticipants, gameParticipationCode)
        val contentsSession = sessionRepository.save(createdContentsSession)
        log.info("콘텐츠 세션 저장 완료: sessionCode={}, streamerId={}", contentsSession.sessionCode, streamerId)
        
        return contentsSession.toResponseDto()
    }
    
    fun modifySessionSettings(
            streamerId: Long,
            maxGroupParticipants: Int,
            gameParticipationCode: String?
    ): ContentsSessionResponseDto {
        log.info(
            "세션 설정 변경 요청: streamerId={}, maxGroupParticipants={}, gameParticipationCode={}",
            streamerId,
            maxGroupParticipants,
            gameParticipationCode
        )
        val contentsSession = sessionQueryService.getOpenContentsSession(streamerId = streamerId)
        log.info("변경 대상 세션 코드: {}", contentsSession.sessionCode)
        val updatedContentsSession = contentsSession.updateGameSettings(gameParticipationCode, maxGroupParticipants).toResponseDto()
        
        sseAdapter.emitStreamerSessionUpdateEventAsync(streamerId, contentsSession, updatedContentsSession)
        log.info("세션 설정 변경 완료: sessionCode={}", updatedContentsSession.sessionCode)
        return updatedContentsSession
    }
    
    fun closeContentsSession(streamerId: Long) {
        log.info("세션 닫기 시도: streamerId={}", streamerId)
        val session = sessionQueryService.getOpenContentsSession(streamerId = streamerId).apply { close() }
        val sessionCode = session.sessionCode
        log.info("닫는 세션 코드: {}", sessionCode)
        
        ParticipantOrderManager.removeParticipantOrderQueue(sessionCode)
        sessionRepository.setAllParticipantsToLeft(sessionCode)
        log.info("참여자 상태 처리 완료: sessionCode={}", sessionCode)
        
        sseAdapter.broadcastEvent(sessionCode, SseEventType.CLOSED_SESSION, null)
        sseEmitterManager.unsubscribeAll(sessionCode)
        log.info("SSE 브로드캐스트 완료 및 구독 해제: sessionCode={}", sessionCode)
    }
    
    fun switchFixedPickStatus(streamerId: Long, viewerId: Long?) {
        log.info("고정픽 상태 변경 시도: streamerId={}, viewerId={}", streamerId, viewerId)
        val validViewerId = requireNotNull(viewerId) { "유효하지 않은 참여자 정보입니다." }
        
        val contentsSession = sessionQueryService.getOpenContentsSession(streamerId = streamerId)
        val chzzkNickname = memberQueryService.getMember(memberId = streamerId).channelName
        
        val participant = sessionQueryService.getParticipant(viewerId = validViewerId, sessionId = contentsSession.id!!)
        participant.toggleFixedPick()
        
        ParticipantOrderManager.addOrUpdateParticipantOrder(contentsSession.sessionCode, participant, validViewerId, chzzkNickname)
        sseAdapter.emitParticipantFixedEventAsync(participant)
        log.info("고정픽 상태 변경 완료: streamerId={}, viewerId={}, 닉네임={}", streamerId, validViewerId, chzzkNickname)
    }
    
    fun proceedToNextGroup(streamerId: Long) {
        val session = sessionQueryService.getOpenContentsSession(streamerId = streamerId)
        log.info("다음 그룹 진행 요청: sessionCode={}", session.sessionCode)
        val participants = sessionRepository.findFirstPartyParticipants(session.id!!, session.maxGroupParticipants)
        log.info("참가자 수: {}", participants.size)
        participants.forEach { participant -> participant.incrementSessionRound() }
        
        ParticipantOrderManager.rotateFirstNOrdersToNextCycle(session)
        sseAdapter.notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, session)
        log.info("그룹 회전 처리 완료: sessionCode={}", session.sessionCode)
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