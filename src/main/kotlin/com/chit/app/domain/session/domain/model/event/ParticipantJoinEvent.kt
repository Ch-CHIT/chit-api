package com.chit.app.domain.session.domain.model.event

data class ParticipantJoinEvent(
        val sessionCode: String,
        val participantId: Long,
        val gameNickname: String
)