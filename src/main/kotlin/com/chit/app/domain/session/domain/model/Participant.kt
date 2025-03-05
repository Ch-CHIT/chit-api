package com.chit.app.domain.session.domain.model

import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Participant(
        val order: Int? = null,
        val round: Int,
        val fixedPick: Boolean,
        val status: ParticipationStatus,
        val viewerId: Long,
        val participantId: Long,
        val chzzkNickname: String,
        val gameNickname: String,
)