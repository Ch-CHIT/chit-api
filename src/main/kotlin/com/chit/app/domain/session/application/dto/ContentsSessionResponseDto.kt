package com.chit.app.domain.session.application.dto

import com.chit.app.domain.session.domain.model.status.SessionStatus
import com.chit.app.global.response.SuccessResponse.PagedResponse
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContentsSessionResponseDto(
        val sessionId: Long? = null,
        val liveId: Long? = null,
        val status: SessionStatus? = null,
        val gameParticipationCode: String? = null,
        val sessionParticipationCode: String? = null,
        val maxParticipants: Int? = null,
        val currentParticipants: Int? = null,
        val participants: PagedResponse<Participant>? = null
)