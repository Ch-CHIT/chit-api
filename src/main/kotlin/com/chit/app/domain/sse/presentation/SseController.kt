package com.chit.app.domain.sse.presentation

import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.session.application.service.HeartBeatService
import com.chit.app.domain.sse.infrastructure.SseAdapter
import com.chit.app.global.common.response.SuccessResponse.Companion.success
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
        private val sseAdapter: SseAdapter,
        private val heartBeatService: HeartBeatService
) {
    
    @GetMapping("/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(
            @CurrentMemberId memberId: Long,
            @RequestParam sessionCode: String,
            @RequestParam(required = false) gameNickname: String?
    ): SseEmitter {
        val emitter = sseAdapter.subscribe(memberId, sessionCode, gameNickname)
        heartBeatService.touch(memberId, sessionCode)
        return emitter
    }
    
    @GetMapping("/heartbeat")
    fun heartbeat(
            @CurrentMemberId memberId: Long,
            @RequestParam sessionCode: String
    ): EmptyResponse {
        heartBeatService.touch(memberId, sessionCode)
        return success()
    }
    
}