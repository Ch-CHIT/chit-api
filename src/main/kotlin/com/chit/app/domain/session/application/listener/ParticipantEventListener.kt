package com.chit.app.domain.session.application.listener

import com.chit.app.domain.session.application.service.handler.ParticipantEventHandler
import com.chit.app.domain.session.domain.model.event.ParticipantExitEvent
import com.chit.app.domain.session.domain.model.event.ParticipantJoinEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

@Component
class ParticipantEventListener(
        private val taskExecutor: ExecutorService,
        private val handler: ParticipantEventHandler,
) {
    
    @EventListener
    fun handleParticipantJoin(event: ParticipantJoinEvent) {
        CompletableFuture.runAsync({ handler.handleParticipantJoin(event.sessionCode, event.participantId, event.gameNickname) }, taskExecutor)
    }
    
    @EventListener
    fun handleParticipantDisconnection(event: ParticipantExitEvent) {
        CompletableFuture.runAsync({ handler.handleParticipantExit(event.sessionCode, event.viewerId) }, taskExecutor)
    }
    
}