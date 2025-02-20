package com.chit.app.domain.session.application.listener

import com.chit.app.domain.session.application.service.ParticipantService
import com.chit.app.domain.session.domain.model.event.ParticipantDisconnectionEvent
import com.chit.app.domain.session.domain.model.event.ParticipantJoinEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

@Component
class ParticipantEventListener(
        private val taskExecutor: ExecutorService,
        private val participantService: ParticipantService,
) {
    
    @EventListener
    fun handleParticipantJoin(event: ParticipantJoinEvent) {
        CompletableFuture.runAsync({ participantService.joinParticipant(event.sessionCode, event.participantId, event.gameNickname) }, taskExecutor)
    }
    
    @EventListener
    fun handleParticipantDisconnection(event: ParticipantDisconnectionEvent) {
        CompletableFuture.runAsync({ participantService.removeParticipant(event.sessionCode, event.viewerId) }, taskExecutor)
    }
    
}