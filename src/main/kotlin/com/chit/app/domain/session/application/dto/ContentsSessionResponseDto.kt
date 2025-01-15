package com.chit.app.domain.session.application.dto

import com.chit.app.global.response.SuccessResponse.PagedResponse
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContentsSessionResponseDto(
        val sessionCode: String? = null,
        val maxGroupParticipants: Int? = null,
        val currentParticipants: Int? = null,
        val gameParticipationCode: String? = null,
        val participants: PagedResponse<Participant>? = null
)