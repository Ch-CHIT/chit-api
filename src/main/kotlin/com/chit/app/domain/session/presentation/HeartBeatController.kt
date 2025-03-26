package com.chit.app.domain.session.presentation

import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.session.application.service.HeartBeatService
import com.chit.app.global.common.response.SuccessResponse.Companion.success
import com.chit.app.global.delegate.Void
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sse")
class HeartBeatController(
        private val heartBeatService: HeartBeatService
) {
    
    @GetMapping("/viewer/heartbeat")
    fun viewerHeartbeat(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @RequestParam sessionCode: String
    ): Void {
        heartBeatService.touchViewer(viewerId, sessionCode)
        return success()
    }
    
    @GetMapping("/streamer/heartbeat")
    fun streamerHeartbeat(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long
    ): Void {
        heartBeatService.touchStreamer(streamerId)
        return success()
    }
    
}