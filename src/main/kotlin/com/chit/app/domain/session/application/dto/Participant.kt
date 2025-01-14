package com.chit.app.domain.session.application.dto

data class Participant(
        val memberId: Long,
        val chzzkNickname: String,
        val gameNickname: String,
        val fixedPick: Boolean
)