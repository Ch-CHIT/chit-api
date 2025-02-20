package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.ParticipantOrder
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.delegate.logger
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
    
    @Transactional
    fun joinParticipant(sessionCode: String, viewerId: Long, gameNickname: String) {
        val session = sessionRepository.findOpenContentsSessionBy(sessionCode)
                ?: throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
        
        if (!sessionRepository.existsParticipantInSession(session.id!!, viewerId)) {
            log.debug("세션 '{}'에 viewerId '{}'가 존재하지 않음. 참가자 추가 진행", session.id, viewerId)
            
            val participant = SessionParticipant.create(viewerId, gameNickname, session)
            sessionRepository.addParticipant(participant)
            log.debug("새 참가자 등록 완료: participant id '{}'", participant.id)
            
            val participantOrder = ParticipantOrder(
                status = participant.status,
                participantId = participant.id!!,
                viewerId = viewerId
            )
            ParticipantOrderManager.addParticipant(sessionCode, participantOrder)
            emitStreamerEvent(session, SseEvent.STREAMER_PARTICIPANT_ADDED)
        } else {
            log.debug("세션 '{}'에 viewerId '{}'가 이미 참가 중임", session.id, viewerId)
        }
        reorderParticipants(sessionCode)
    }
    
    @Transactional
    fun removeParticipant(sessionCode: String, viewerId: Long) {
        val session = sessionRepository.findParticipantBy(viewerId, sessionCode)
                ?.apply { status = ParticipationStatus.LEFT }
                ?.let { participant -> participant.contentsSession.apply { removeParticipant() } }
                ?: return
        
        ParticipantOrderManager.removeParticipant(sessionCode, viewerId)
        emitStreamerEvent(session, SseEvent.STREAMER_PARTICIPANT_REMOVED)
        reorderParticipants(sessionCode)
    }
    
    private fun emitStreamerEvent(session: ContentsSession, event: SseEvent) {
        runAsync({
            val data = mapOf(
                "maxGroupParticipants" to session.maxGroupParticipants,
                "currentParticipants" to session.currentParticipants
            )
            streamerSseService.emitStreamerEvent(session.streamerId, event, data)
        }, taskExecutor)
    }
    
    private fun reorderParticipants(sessionCode: String) {
        runAsync({
            sessionSseService.reorderSessionParticipants(sessionCode, SseEvent.PARTICIPANT_ORDER_UPDATED)
        }, taskExecutor)
    }
}