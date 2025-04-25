package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.domain.exception.InvalidParticipantException
import com.chit.app.domain.session.domain.exception.SessionParticipantNotFoundException
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.sse.infrastructure.SseAdapter
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ParticipantService(
        private val sseAdapter: SseAdapter,
        private val sessionRepository: SessionRepository
) {
    
    private val log = logger<ParticipantService>()
    
    @Transactional
    fun leaveSession(sessionCode: String, viewerId: Long) {
        val participant = sessionRepository.findParticipantBy(viewerId, sessionCode) ?: throw InvalidParticipantException()
        
        participant.leaveContentSession()
        sseAdapter.emitExitEventAsync(participant)
        log.info("참여자 퇴장 처리 완료 - 시청자ID: $viewerId, 세션코드: $sessionCode")
    }
    
    @Transactional
    fun kickParticipant(streamerId: Long, viewerId: Long?) {
        require(viewerId != null) { "유효하지 않은 참여자 정보입니다." }
        val participant = sessionRepository.findParticipantBy(viewerId, streamerId = streamerId)
                ?.apply { leaveContentSession() }
                ?: throw SessionParticipantNotFoundException()
        
        sseAdapter.emitKickEventAsync(participant)
    }
    
    private fun SessionParticipant.leaveContentSession() {
        status = ParticipationStatus.LEFT
        contentsSession.removeParticipant()
    }
    
}