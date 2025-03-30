package com.chit.app.global.shutdown

import com.chit.app.domain.session.infrastructure.JpaContentsSessionRepository
import com.chit.app.domain.session.infrastructure.JpaSessionParticipantRepository
import com.chit.app.domain.sse.infrastructure.SseEmitterManager
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class ApplicationShutdownListener(
        private val sseEmitterManager: SseEmitterManager,
        private val sessionRepository: JpaContentsSessionRepository,
        private val participantRepository: JpaSessionParticipantRepository,
) : ApplicationListener<ContextClosedEvent> {
    
    override fun onApplicationEvent(event: ContextClosedEvent) {
        sseEmitterManager.completeAllEmitters()
        sessionRepository.closeAllOpenSessions()
        participantRepository.closeAllNonLeftParticipants()
    }
    
}