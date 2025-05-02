package com.chit.app.domain.session.presentation

import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.application.service.ParticipantService
import com.chit.app.domain.session.application.service.SessionCommandService
import com.chit.app.domain.session.application.service.SessionQueryService
import com.chit.app.domain.session.presentation.dto.ContentsSessionUpsertRequestDto
import com.chit.app.global.delegate.EmptyResponse
import com.chit.app.global.response.SuccessResponse
import com.chit.app.global.response.SuccessResponse.Companion.success
import com.chit.app.global.response.SuccessResponse.Companion.successWithData
import io.swagger.v3.oas.annotations.Parameter
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

typealias GameCodeResponse = ResponseEntity<SuccessResponse<ContentsSessionResponseDto?>>
typealias DetailContentsSessionResponse = ResponseEntity<SuccessResponse<ContentsSessionResponseDto?>>
typealias NewContentsSessionResponse = ResponseEntity<SuccessResponse<ContentsSessionResponseDto?>>

@RestController
@RequestMapping("/api/v1/contents/session")
class SessionController(
        private val sessionCommandService: SessionCommandService,
        private val sessionQueryService: SessionQueryService,
        private val participantService: ParticipantService
) {
    
    @PostMapping
    fun createContentsSession(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @RequestBody requestDto: ContentsSessionUpsertRequestDto
    ): NewContentsSessionResponse {
        val (maxGroupParticipants, gameParticipationCode) = requestDto
        return successWithData(sessionCommandService.createContentsSession(streamerId, gameParticipationCode, maxGroupParticipants))
    }
    
    @GetMapping
    fun getContentsSessionWithParticipants(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @PageableDefault(page = 0, size = 20) pageable: Pageable
    ): DetailContentsSessionResponse {
        val contentsSessionDetail = sessionQueryService.getContentsSessionWithParticipants(streamerId, pageable)
        return successWithData(contentsSessionDetail)
    }
    
    @PutMapping
    fun modifySessionSettings(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
            @RequestBody requestDto: ContentsSessionUpsertRequestDto
    ): DetailContentsSessionResponse {
        val updatedSession = sessionCommandService.modifySessionSettings(
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
        sessionCommandService.closeContentsSession(streamerId)
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
        sessionCommandService.switchFixedPickStatus(streamerId, viewerId)
        return success()
    }
    
    @PutMapping("/next-group")
    fun proceedToNextGroup(
            @Parameter(hidden = true) @CurrentMemberId streamerId: Long,
    ): EmptyResponse {
        sessionCommandService.proceedToNextGroup(streamerId)
        return success()
    }
    
    @GetMapping("/{sessionCode}/gameCode")
    fun getGameParticipationCode(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @PathVariable sessionCode: String
    ): GameCodeResponse {
        return successWithData(sessionQueryService.getGameParticipationCode(sessionCode, viewerId))
    }
    
    @DeleteMapping("/{sessionCode}/leave")
    fun leaveSession(
            @Parameter(hidden = true) @CurrentMemberId viewerId: Long,
            @PathVariable sessionCode: String
    ): EmptyResponse {
        participantService.leaveSession(sessionCode, viewerId)
        return success()
    }
    
}