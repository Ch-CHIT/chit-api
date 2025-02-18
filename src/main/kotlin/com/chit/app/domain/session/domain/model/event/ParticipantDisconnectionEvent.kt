package com.chit.app.domain.session.domain.model.event

data class ParticipantDisconnectionEvent(
        val sessionCode: String,
        val viewerId: Long
)