package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.domain.repository.SessionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HeartBeatService(
        private val sessionService: SessionService,
        private val participantService: ParticipantService,
        private val sessionRepository: SessionRepository
) {
    
    @Transactional
    fun processHeartbeatTimeout(sessionCode: String, memberId: Long) {
        val contentsSession = sessionRepository.findOpenContentsSessionBy(sessionCode = sessionCode) ?: return
        if (contentsSession.streamerId == memberId) {
            sessionService.closeContentsSession(memberId)
        } else {
            participantService.leaveSession(sessionCode, memberId)
        }
    }
    
}