package com.chit.app.domain.session.domain.model

data class Participant(
        val memberId: Long,
        val chzzkNickname: String,
        val gameNickname: String,
        val fixedPick: Boolean,
        val round: Int
)