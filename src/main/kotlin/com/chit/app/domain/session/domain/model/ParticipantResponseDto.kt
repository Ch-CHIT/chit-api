package com.chit.app.domain.session.domain.model

data class ParticipantResponseDto(
        val viewerId: Long,
        val chzzkNickname: String,
        val gameNickname: String,
        val fixedPick: Boolean,
        val round: Int
)