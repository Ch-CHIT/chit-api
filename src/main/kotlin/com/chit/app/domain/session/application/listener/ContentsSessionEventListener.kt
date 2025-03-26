package com.chit.app.domain.session.application.listener

import com.chit.app.domain.session.application.service.SessionService
import com.chit.app.domain.session.domain.model.event.SessionCloseEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

@Component
class ContentsSessionEventListener(
        private val taskExecutor: ExecutorService,
        private val sessionService: SessionService
) {
    
    @EventListener
    fun onContentsSessionClose(event: SessionCloseEvent) {
        CompletableFuture.runAsync({ sessionService.closeContentsSession(event.streamerId) }, taskExecutor)
    }
    
}