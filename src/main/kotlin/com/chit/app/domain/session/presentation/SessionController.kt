package com.chit.app.domain.session.presentation

import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.session.application.service.SessionService
import com.chit.app.domain.session.presentation.dto.ContentsSessionUpsertRequestDto
import com.chit.app.global.common.response.SuccessResponse.Companion.success
import com.chit.app.global.common.response.SuccessResponse.Companion.successWithData
import com.chit.app.global.delegate.DetailContentsSession
import com.chit.app.global.delegate.GameCode
import com.chit.app.global.delegate.NewContentsSession
import com.chit.app.global.delegate.Void
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/contents/session")
class SessionController(
        private val sessionService: SessionService
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
    fun getOpeningContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PageableDefault(page = 0, size = 20) pageable: Pageable
    ): DetailContentsSession {
        val contentsSessionDetail = sessionService.getOpeningContentsSession(streamerId, pageable)
        return successWithData(contentsSessionDetail)
    }
    
    @PutMapping
    fun updateSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @RequestBody requestDto: ContentsSessionUpsertRequestDto
    ): DetailContentsSession {
        val updatedContentsSession = sessionService.modifySessionSettings(
            streamerId,
            requestDto.maxGroupParticipants,
            requestDto.gameParticipationCode
        )
        return successWithData(updatedContentsSession)
    }
    
    @DeleteMapping
    fun closeContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long
    ): Void {
        sessionService.closeContentsSession(streamerId)
        return success()
    }
    
    @DeleteMapping("/participants/{viewerId}")
    fun publishDisconnectionNotification(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PathVariable("viewerId") viewerId: Long?
    ): Void {
        sessionService.publishDisconnectionNotification(streamerId, viewerId)
        return success()
    }
    
    @PutMapping("/participants/{viewerId}/pick")
    fun switchFixedPickStatus(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PathVariable("viewerId") viewerId: Long?
    ): Void {
        sessionService.switchFixedPickStatus(streamerId, viewerId)
        return success()
    }
    
    @PutMapping("/next-group")
    fun proceedToNextGroup(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
    ): Void {
        sessionService.proceedToNextGroup(streamerId)
        return success()
    }
    
    @GetMapping("/{sessionCode}/gameCode")
    fun retrieveGameParticipationCode(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @PathVariable sessionCode: String
    ): GameCode {
        return successWithData(sessionService.retrieveGameParticipationCode(sessionCode, viewerId))
    }
    
    @DeleteMapping("/{sessionCode}/leave")
    fun exitContentsSession(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @PathVariable sessionCode: String
    ): Void {
        sessionService.exitContentsSession(viewerId, sessionCode)
        return success()
    }
    
}