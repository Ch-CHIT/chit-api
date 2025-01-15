package com.chit.app.domain.session.presentation

import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.session.application.sse.SessionSseService
import com.chit.app.domain.session.application.sse.StreamerSseService
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
        private val streamerSseService: StreamerSseService,
        private val sessionSseService: SessionSseService
) {
    
    @GetMapping("/streamer/init", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun init(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long
    ): SseEmitter {
        return streamerSseService.subscribe(streamerId)
    }
    
    @GetMapping("/viewer/subscribe", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun subscribe(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @RequestParam sessionParticipationCode: String,
            @RequestParam gameNickname: String
    ): SseEmitter {
        return sessionSseService.subscribe(viewerId, sessionParticipationCode, gameNickname)
    }
    
}