package com.chit.app.domain.session.presentation

import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.session.application.SessionService
import com.chit.app.domain.session.presentation.dto.ContentsSessionUpsertRequestDto
import com.chit.app.global.delegate.DetailContentsSession
import com.chit.app.global.delegate.NewContentsSession
import com.chit.app.global.delegate.Void
import com.chit.app.global.response.SuccessResponse.Companion.success
import com.chit.app.global.response.SuccessResponse.Companion.successWithData
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.domain.Pageable
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/contents/session")
class SessionController(
        private val sessionService: SessionService
) {
    
    @PostMapping
    fun createContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @RequestBody request: ContentsSessionUpsertRequestDto
    ): NewContentsSession {
        val createdContentsSession = sessionService.createContentsSession(
            streamerId = streamerId,
            maxGroupParticipants = request.maxParticipantCount,
            gameParticipationCode = request.gameParticipationCode
        )
        return successWithData(createdContentsSession)
    }
    
    @GetMapping
    fun getCurrentOpeningContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            pageable: Pageable
    ): DetailContentsSession {
        val contentsSessionDetail = sessionService.getCurrentOpeningContentsSession(streamerId, pageable)
        return successWithData(contentsSessionDetail)
    }
    
    @PutMapping
    fun updateSession(
            @Parameter(hidden = true) @CurrentMemberId currentMemberId: Long,
            @RequestBody request: ContentsSessionUpsertRequestDto
    ): DetailContentsSession {
        val updatedContentsSession = sessionService.updateContentsSession(
            streamerId = currentMemberId,
            maxGroupParticipants = request.maxParticipantCount,
            gameParticipationCode = request.gameParticipationCode
        )
        return successWithData(updatedContentsSession)
    }
    
    @DeleteMapping
    fun closeSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long
    ): Void {
        sessionService.closeContentsSession(streamerId)
        return success()
    }
    
    @DeleteMapping("/participants/{participantId}")
    fun removeParticipantFromSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PathVariable("participantId") participantId: Long
    ): Void {
        sessionService.removeParticipantFromSession(streamerId, participantId)
        return success()
    }
    
    @PutMapping("/participants/{participantId}/pick")
    fun togglePick(
            @PathVariable("participantId") participantId: Long,
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long
    ): Void {
        sessionService.togglePick(streamerId, participantId)
        return success()
    }
    
}