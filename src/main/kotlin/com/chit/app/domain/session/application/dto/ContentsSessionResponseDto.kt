package com.chit.app.domain.session.application.dto

import com.chit.app.domain.session.domain.model.ParticipantResponseDto
import com.chit.app.global.common.response.SuccessResponse.PagedResponse
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContentsSessionResponseDto(
        val streamerId: Long? = null,
        val sessionCode: String? = null,
        val maxGroupParticipants: Int? = null,
        val currentParticipants: Int? = null,
        val gameParticipationCode: String? = null,
        val participants: PagedResponse<ParticipantResponseDto>? = null
) {
    override fun toString(): String {
        return listOfNotNull(
            streamerId?.let { "streamerId=$it" },
            sessionCode?.let { "sessionCode='$it'" },
            maxGroupParticipants?.let { "maxGroupParticipants=$it" },
            currentParticipants?.let { "currentParticipants=$it" },
            gameParticipationCode?.let { "gameParticipationCode='$it'" },
            participants?.let { "participants=$it" }
        ).joinToString(prefix = "ContentsSessionResponseDto(", postfix = ")")
    }
}