package com.chit.app.domain.session.domain.model

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Participant(
        val viewerId: Long,
        val round: Int,
        val fixedPick: Boolean,
        val gameNickname: String,
        val chzzkNickname: String? = null
)