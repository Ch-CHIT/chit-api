package com.chit.app.domain.session.application.dto

import com.chit.app.domain.session.domain.model.status.SessionStatus
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.domain.Page

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ContentsSessionResponseDto(
        val id: Long? = null,
        val status: SessionStatus? = null,
        val liveId: Long? = null,
        val gameParticipationCode: String? = null,
        val sessionParticipationCode: String? = null,
        val maxParticipants: Int? = null,
        val currentParticipants: Int? = null,
        val participants: Page<Participant>? = null
)