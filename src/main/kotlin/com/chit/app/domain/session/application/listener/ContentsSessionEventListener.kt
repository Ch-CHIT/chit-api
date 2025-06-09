package com.chit.app.domain.session.application.listener

import com.chit.app.domain.session.application.service.HeartBeatService
import com.chit.app.domain.session.application.service.SessionCommandService
import com.chit.app.domain.session.domain.model.event.HeartbeatTimeoutEvent
import com.chit.app.domain.session.domain.model.event.SessionCloseEvent
import org.springframework.context.event.EventListener
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class ContentsSessionEventListener(
        private val sessionCommandService: SessionCommandService,
        private val heartBeatService: HeartBeatService
) {
    
    @Async
    @EventListener
    fun onContentsSessionClose(event: SessionCloseEvent) {
        sessionCommandService.closeContentsSession(event.streamerId)
    }
    
    @Async
    @EventListener
    fun handleHeartbeatTimeout(event: HeartbeatTimeoutEvent) {
        heartBeatService.processHeartbeatTimeout(event.sessionCode, event.memberId)
    }
    
}