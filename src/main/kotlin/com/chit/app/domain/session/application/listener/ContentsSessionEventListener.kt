package com.chit.app.domain.session.application.listener

import com.chit.app.domain.session.application.service.SessionService
import com.chit.app.domain.session.domain.model.event.SessionCloseEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ContentsSessionEventListener(
        private val sessionService: SessionService
) {
    
    @Async
    @EventListener
    fun onContentsSessionClose(event: SessionCloseEvent) {
        sessionService.closeContentsSession(event.streamerId)
    }
    
}