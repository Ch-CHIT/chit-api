package com.chit.app.domain.session.application.event

import org.springframework.context.ApplicationEvent

data class ParticipantDisconnectionEvent(
        val sessionCode: String,
        val participantId: Long
) : ApplicationEvent(sessionCode)