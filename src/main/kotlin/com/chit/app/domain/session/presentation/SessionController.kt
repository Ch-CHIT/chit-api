package com.chit.app.domain.session.presentation

import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.session.application.service.ParticipantService
import com.chit.app.domain.session.application.service.SessionService
import com.chit.app.domain.session.presentation.dto.ContentsSessionUpsertRequestDto
import com.chit.app.global.common.response.SuccessResponse.Companion.success
import com.chit.app.global.common.response.SuccessResponse.Companion.successWithData
import com.chit.app.global.delegate.DetailContentsSessionResponse
import com.chit.app.global.delegate.EmptyResponse
import com.chit.app.global.delegate.GameCodeResponse
import com.chit.app.global.delegate.NewContentsSessionResponse
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/contents/session")
class SessionController(
        private val sessionService: SessionService,
        private val participantService: ParticipantService
) {
    
    @PostMapping
    fun createContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @RequestBody requestDto: ContentsSessionUpsertRequestDto
    ): NewContentsSessionResponse {
        val (maxGroupParticipants, gameParticipationCode) = requestDto
        return successWithData(sessionService.createContentsSession(streamerId, gameParticipationCode, maxGroupParticipants))
    }
    
    @GetMapping
    fun getOpeningContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PageableDefault(page = 0, size = 20) pageable: Pageable
    ): DetailContentsSessionResponse {
        val contentsSessionDetail = sessionService.getOpeningContentsSession(streamerId, pageable)
        return successWithData(contentsSessionDetail)
    }
    
    @PutMapping
    fun updateSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @RequestBody requestDto: ContentsSessionUpsertRequestDto
    ): DetailContentsSessionResponse {
        val updatedSession = sessionService.modifySessionSettings(
            streamerId,
            requestDto.maxGroupParticipants,
            requestDto.gameParticipationCode
        )
        return successWithData(updatedSession)
    }
    
    @DeleteMapping
    fun closeContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long
    ): EmptyResponse {
        sessionService.closeContentsSession(streamerId)
        return success()
    }
    
    @DeleteMapping("/participants/{viewerId}")
    fun kickParticipant(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PathVariable("viewerId") viewerId: Long?
    ): EmptyResponse {
        participantService.kickParticipant(streamerId, viewerId)
        return success()
    }
    
    @PutMapping("/participants/{viewerId}/pick")
    fun switchFixedPickStatus(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PathVariable("viewerId") viewerId: Long?
    ): EmptyResponse {
        sessionService.switchFixedPickStatus(streamerId, viewerId)
        return success()
    }
    
    @PutMapping("/next-group")
    fun proceedToNextGroup(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
    ): EmptyResponse {
        sessionService.proceedToNextGroup(streamerId)
        return success()
    }
    
    @GetMapping("/{sessionCode}/gameCode")
    fun retrieveGameParticipationCode(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @PathVariable sessionCode: String
    ): GameCodeResponse {
        return successWithData(sessionService.retrieveGameParticipationCode(sessionCode, viewerId))
    }
    
    @DeleteMapping("/{sessionCode}/leave")
    fun exitContentsSession(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @PathVariable sessionCode: String
    ): EmptyResponse {
        participantService.leaveSession(sessionCode, viewerId)
        return success()
    }
    
}