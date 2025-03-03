package com.chit.app.domain.session.domain.model.event

data class ParticipantExitEvent(
        val sessionCode: String,
        val viewerId: Long
)