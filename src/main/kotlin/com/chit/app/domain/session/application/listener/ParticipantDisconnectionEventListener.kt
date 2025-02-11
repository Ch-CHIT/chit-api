package com.chit.app.domain.session.application.listener

import com.chit.app.domain.session.application.SessionService
import com.chit.app.domain.session.application.event.ParticipantDisconnectionEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ParticipantDisconnectionEventListener(
    private val sessionService: SessionService,
) {
    
    @Async("virtualThreadExecutor")
    @EventListener
    fun handleParticipantDisconnection(event: ParticipantDisconnectionEvent) {
        sessionService.rejectParticipant(event.sessionCode, event.participantId)
    }
    
}