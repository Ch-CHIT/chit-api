package com.chit.app.domain.session.presentation

import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.application.service.SessionService
import com.chit.app.domain.session.application.service.StreamerSseService
import com.chit.app.domain.session.presentation.dto.ContentsSessionUpsertRequestDto
import com.chit.app.global.delegate.DetailContentsSession
import com.chit.app.global.delegate.GameCode
import com.chit.app.global.delegate.NewContentsSession
import com.chit.app.global.delegate.Void
import com.chit.app.global.response.SuccessResponse.Companion.success
import com.chit.app.global.response.SuccessResponse.Companion.successWithData
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/contents/session")
class SessionController(
        private val sessionService: SessionService,
        private val streamerSseService: StreamerSseService,
) {
    
    @PostMapping
    fun createContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @RequestBody requestDto: ContentsSessionUpsertRequestDto
    ): NewContentsSession {
        val (gameParticipationCode, maxGroupParticipants) = requestDto
        val createdContentsSession = sessionService.createContentsSession(streamerId, gameParticipationCode, maxGroupParticipants)
        return successWithData(createdContentsSession)
    }
    
    @GetMapping
    fun getCurrentOpeningContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PageableDefault(page = 0, size = 20) pageable: Pageable
    ): DetailContentsSession {
        val contentsSessionDetail = sessionService.getCurrentOpeningContentsSession(streamerId, pageable)
        return successWithData(contentsSessionDetail)
    }
    
    @PutMapping
    fun updateSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @RequestBody requestDto: ContentsSessionUpsertRequestDto
    ): DetailContentsSession {
        val updatedContentsSession = sessionService.updateContentsSession(
            streamerId,
            requestDto.maxGroupParticipants,
            requestDto.gameParticipationCode
        )
        streamerSseService.emitStreamerEvent(streamerId, SseEvent.STREAMER_SESSION_UPDATED, updatedContentsSession)
        return successWithData(updatedContentsSession)
    }
    
    @DeleteMapping
    fun closeSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long
    ): Void {
        sessionService.closeContentsSession(streamerId)
        return success()
    }
    
    @DeleteMapping("/participants/{viewerId}")
    fun removeParticipantFromSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PathVariable("viewerId") viewerId: Long?
    ): Void {
        sessionService.processParticipantRemoval(streamerId, viewerId)
        return success()
    }
    
    @PutMapping("/participants/{viewerId}/pick")
    fun togglePick(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PathVariable("viewerId") viewerId: Long?
    ): Void {
        sessionService.togglePick(streamerId, viewerId)
        return success()
    }
    
    @GetMapping("/{sessionCode}/gameCode")
    fun getGameParticipationCode(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @PathVariable sessionCode: String
    ): GameCode {
        return successWithData(sessionService.getGameParticipationCode(sessionCode, viewerId))
    }
    
    @DeleteMapping("/leave")
    fun leaveSession(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @RequestParam sessionCode: String
    ): Void {
        sessionService.leaveSession(viewerId, sessionCode)
        return success()
    }
    
}