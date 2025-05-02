package com.chit.app.domain.sse.presentation

import com.chit.app.global.common.annotation.CurrentMemberId
import com.chit.app.domain.session.application.scheduler.SessionHeartbeatScheduler
import com.chit.app.domain.sse.application.SseSubscribeService
import com.chit.app.global.response.SuccessResponse.Companion.success
import com.chit.app.global.delegate.EmptyResponse
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/sse/session")
class SseController(
        private val sseSubscribeService: SseSubscribeService,
        private val sessionHeartbeatScheduler: SessionHeartbeatScheduler
) {
    
    @GetMapping("/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(
            @CurrentMemberId memberId: Long,
            @RequestParam sessionCode: String,
            @RequestParam(required = false) gameNickname: String?
    ): SseEmitter {
        val emitter = sseSubscribeService.subscribe(memberId, sessionCode, gameNickname)
        sessionHeartbeatScheduler.touch(memberId, sessionCode)
        return emitter
    }
    
    @GetMapping("/heartbeat")
    fun heartbeat(
            @CurrentMemberId memberId: Long,
            @RequestParam sessionCode: String
    ): EmptyResponse {
        sessionHeartbeatScheduler.touch(memberId, sessionCode)
        return success()
    }
    
}