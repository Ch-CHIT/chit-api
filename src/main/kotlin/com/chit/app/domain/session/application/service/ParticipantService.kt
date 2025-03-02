package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.annotation.LogExecutionTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.ExecutorService

@Service
class ParticipantService(
        private val taskExecutor: ExecutorService,
        private val sessionRepository: SessionRepository,
        private val sessionSseService: SessionSseService,
        private val streamerSseService: StreamerSseService,
) {
    
    @LogExecutionTime
    @Transactional
    fun joinParticipant(sessionCode: String, viewerId: Long, gameNickname: String) {
        val session = findOpenSessionOrThrow(sessionCode)
        if (isViewerNotInSession(session.id!!, viewerId)) {
            registerParticipant(session, viewerId, gameNickname)
        }
        
        reorderParticipants(session)
    }
    
    @LogExecutionTime
    @Transactional
    fun removeParticipant(sessionCode: String, viewerId: Long) {
        val participant = sessionRepository.findParticipantBy(viewerId, sessionCode)
                ?.apply {
                    status = ParticipationStatus.LEFT
                    contentsSession.removeParticipant()
                    cleanupAfterRemoval()
                } ?: throw IllegalArgumentException("해당 세션 참여 정보를 확인할 수 없습니다. 다시 시도해 주세요.")
        
        reorderParticipants(participant.contentsSession)
    }
    
    private fun SessionParticipant.cleanupAfterRemoval() {
        sessionSseService.disconnectSseEmitter(contentsSession.sessionCode, viewerId)
        ParticipantOrderManager.removeParticipantOrder(contentsSession.sessionCode, viewerId)
        emitStreamerEvent(SseEvent.STREAMER_PARTICIPANT_REMOVED, contentsSession)
    }
    
    private fun registerParticipant(session: ContentsSession, viewerId: Long, gameNickname: String) {
        val participant = SessionParticipant.create(viewerId, gameNickname, session)
        sessionRepository.addParticipant(participant).apply { contentsSession.addParticipant() }
        ParticipantOrderManager.addOrUpdateParticipantOrder(session.sessionCode, participant, viewerId)
        emitStreamerEvent(SseEvent.STREAMER_PARTICIPANT_ADDED, session)
    }
    
    private fun findOpenSessionOrThrow(sessionCode: String): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(sessionCode)
                ?: throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
    }
    
    private fun isViewerNotInSession(sessionId: Long, viewerId: Long): Boolean {
        return !sessionRepository.existsParticipantInSession(sessionId, viewerId)
    }
    
    private fun emitStreamerEvent(event: SseEvent, session: ContentsSession) {
        runAsync({
            val data = mapOf(
                "maxGroupParticipants" to session.maxGroupParticipants,
                "currentParticipants" to session.currentParticipants
            )
            streamerSseService.emitStreamerEvent(session.streamerId, data, event)
        }, taskExecutor)
    }
    
    private fun reorderParticipants(session: ContentsSession) {
        runAsync({
            sessionSseService.reorderSessionParticipants(
                session.sessionCode,
                session.gameParticipationCode,
                session.maxGroupParticipants,
                SseEvent.PARTICIPANT_ORDER_UPDATED
            )
        }, taskExecutor)
    }
    
}