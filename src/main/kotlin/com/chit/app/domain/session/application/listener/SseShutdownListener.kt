package com.chit.app.domain.session.application.listener

import com.chit.app.domain.session.application.sse.SessionSseService
import com.chit.app.domain.session.application.sse.StreamerSseService
import com.chit.app.domain.session.infrastructure.JpaContentsSessionRepository
import kotlinx.coroutines.runBlocking
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component

@Component
class SseShutdownListener(
        private val sessionRepository: JpaContentsSessionRepository,
        private val streamerSseService: StreamerSseService,
        private val sessionSseService: SessionSseService,
) : ApplicationListener<ContextClosedEvent> {
    
    override fun onApplicationEvent(event: ContextClosedEvent) {
        runBlocking {
            streamerSseService.closeAllSessions()
            sessionSseService.clearAllSessions()
            sessionRepository.closeAllOpenSessions()
        }
    }
    
}