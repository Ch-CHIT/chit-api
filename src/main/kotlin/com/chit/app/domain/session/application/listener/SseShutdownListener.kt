package com.chit.app.domain.session.application.listener

import com.chit.app.domain.session.application.service.SessionSseService
import com.chit.app.domain.session.application.service.StreamerSseService
import com.chit.app.domain.session.infrastructure.JpaContentsSessionRepository
import com.chit.app.domain.session.infrastructure.JpaSessionParticipantRepository
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class SseShutdownListener(
        private val sessionSseService: SessionSseService,
        private val streamerSseService: StreamerSseService,
        private val sessionRepository: JpaContentsSessionRepository,
        private val participantRepository: JpaSessionParticipantRepository,
) : ApplicationListener<ContextClosedEvent> {
    
    override fun onApplicationEvent(event: ContextClosedEvent) {
        streamerSseService.closeAllSessionEmitters()
        sessionSseService.clearAllParticipantEmitters()
        sessionRepository.closeAllOpenSessions()
        participantRepository.closeAllNonLeftParticipants()
    }
    
}