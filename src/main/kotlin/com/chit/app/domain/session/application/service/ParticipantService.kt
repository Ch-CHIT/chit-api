package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.annotation.LogExecutionTime
import com.chit.app.global.common.logging.logger
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
    
    private val log = logger<ParticipantService>()
    
    @LogExecutionTime
    @Transactional
    fun joinParticipant(sessionCode: String, viewerId: Long, gameNickname: String) {
        val session = findOpenSessionOrThrow(sessionCode)
        if (isViewerAlreadyParticipant(session.id!!, viewerId)) {
            log.debug("세션 '{}'에 viewerId '{}'가 이미 참가 중임", session.id, viewerId)
        } else {
            registerNewParticipant(session, viewerId, gameNickname)
            log.debug("새 참가자 등록 완료: '{}'", viewerId)
        }
        reorderParticipants(sessionCode, session.gameParticipationCode, session.maxGroupParticipants)
    }
    
    @LogExecutionTime
    @Transactional
    fun removeParticipant(sessionCode: String, viewerId: Long) {
        val session = sessionRepository.findParticipantBy(viewerId, sessionCode)
                ?.apply { status = ParticipationStatus.LEFT }
                ?.let { participant -> participant.contentsSession.apply { removeParticipant() } }
                ?: return
        
        ParticipantOrderManager.removeParticipantOrder(sessionCode, viewerId)
        sessionSseService.disconnectSseEmitter(sessionCode, viewerId)
        emitStreamerEvent(session, SseEvent.STREAMER_PARTICIPANT_REMOVED)
        reorderParticipants(sessionCode, session.gameParticipationCode, session.maxGroupParticipants)
    }
    
    private fun registerNewParticipant(session: ContentsSession, viewerId: Long, gameNickname: String) {
        SessionParticipant.create(viewerId, gameNickname, session).also { participant ->
            sessionRepository.addParticipant(participant).apply { contentsSession.addParticipant() }
            ParticipantOrderManager.addOrUpdateParticipantOrder(session.sessionCode, participant, viewerId)
        }
        emitStreamerEvent(session, SseEvent.STREAMER_PARTICIPANT_ADDED)
    }
    
    private fun findOpenSessionOrThrow(sessionCode: String): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(sessionCode)
                ?: throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
    }
    
    private fun isViewerAlreadyParticipant(sessionId: Long, viewerId: Long): Boolean {
        return sessionRepository.existsParticipantInSession(sessionId, viewerId)
    }
    
    private fun emitStreamerEvent(session: ContentsSession, event: SseEvent) {
        runAsync({
            val data = mapOf(
                "maxGroupParticipants" to session.maxGroupParticipants,
                "currentParticipants" to session.currentParticipants
            )
            streamerSseService.emitStreamerEvent(session.streamerId, data, event)
        }, taskExecutor)
    }
    
    private fun reorderParticipants(sessionCode: String, gameParticipationCode: String?, maxGroupParticipants: Int) {
        runAsync({
            sessionSseService.reorderSessionParticipants(
                sessionCode,
                gameParticipationCode,
                maxGroupParticipants,
                SseEvent.PARTICIPANT_ORDER_UPDATED
            )
        }, taskExecutor)
    }
    
}