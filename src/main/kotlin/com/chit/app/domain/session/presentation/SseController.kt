package com.chit.app.domain.session.presentation

import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.session.application.service.HeartBeatService
import com.chit.app.domain.session.application.service.SessionSseService
import com.chit.app.domain.session.application.service.StreamerSseService
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@RestController
@RequestMapping("/api/v1/sse/session")
class SseController(
        private val heartBeatService: HeartBeatService,
        private val sessionSseService: SessionSseService,
        private val streamerSseService: StreamerSseService
) {
    
    @GetMapping("/streamer/init", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun init(@Parameter(hidden = true) @CurrentMemberId streamerId: Long): SseEmitter {
        val emitter = streamerSseService.subscribe(streamerId)
        heartBeatService.touchStreamer(streamerId)
        return emitter
    }
    
    @GetMapping("/viewer/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @RequestParam sessionCode: String,
            @RequestParam gameNickname: String
    ): SseEmitter {
        val emitter = sessionSseService.subscribe(viewerId, sessionCode, gameNickname)
        heartBeatService.touchViewer(viewerId, sessionCode)
        return emitter
    }
    
}