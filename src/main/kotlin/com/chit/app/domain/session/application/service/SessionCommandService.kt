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
        val liveStream = liveStreamCommandService.saveOrUpdate(streamerId)
        val liveId = liveStream.liveId!!
        
        if (!sessionRepository.notExistsOpenContentsSession(liveId)) {
            throw DuplicateContentsSessionException()
        }
        
        val createdContentsSession = ContentsSession.create(liveId, streamerId, maxGroupParticipants, gameParticipationCode)
        val contentsSession = sessionRepository.save(createdContentsSession)
        
        log.info("컨텐츠 세션 생성 완료: 세션코드: ${contentsSession.sessionCode}, 스트리머ID: $streamerId, 채널ID: $liveStream.channelId")
        return contentsSession.toResponseDto()
    }
    
    fun modifySessionSettings(
            streamerId: Long,
            maxGroupParticipants: Int,
            gameParticipationCode: String?
    ): ContentsSessionResponseDto {
        val contentsSession = sessionQueryService.getOpenContentsSession(streamerId = streamerId)
        val updatedContentsSession = contentsSession.updateGameSettings(gameParticipationCode, maxGroupParticipants).toResponseDto()
        
        sseAdapter.emitStreamerSessionUpdateEventAsync(streamerId, contentsSession, updatedContentsSession)
        return updatedContentsSession
    }
    
    fun closeContentsSession(streamerId: Long) {
        val session = sessionQueryService.getOpenContentsSession(streamerId = streamerId).apply { close() }
        val sessionCode = session.sessionCode
        
        ParticipantOrderManager.removeParticipantOrderQueue(sessionCode)
        sessionRepository.setAllParticipantsToLeft(sessionCode)
        
        sseAdapter.broadcastEvent(sessionCode, SseEventType.CLOSED_SESSION, null)
        sseEmitterManager.unsubscribeAll(sessionCode)
    }
    
    fun switchFixedPickStatus(streamerId: Long, viewerId: Long?) {
        val validViewerId = requireNotNull(viewerId) { "유효하지 않은 참여자 정보입니다." }
        
        val contentsSession = sessionQueryService.getOpenContentsSession(streamerId = streamerId)
        val chzzkNickname = memberQueryService.getMember(memberId = streamerId).channelName
        
        val participant = sessionQueryService.getParticipant(viewerId = validViewerId, sessionId = contentsSession.id!!)
        participant.toggleFixedPick()
        
        ParticipantOrderManager.addOrUpdateParticipantOrder(contentsSession.sessionCode, participant, validViewerId, chzzkNickname)
        sseAdapter.emitParticipantFixedEventAsync(participant)
    }
    
    fun proceedToNextGroup(streamerId: Long) {
        val session = sessionQueryService.getOpenContentsSession(streamerId = streamerId)
        val participants = sessionRepository.findFirstPartyParticipants(session.id!!, session.maxGroupParticipants)
        participants.forEach { participant -> participant.incrementSessionRound() }
        
        ParticipantOrderManager.rotateFirstNOrdersToNextCycle(session)
        sseAdapter.notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, session)
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