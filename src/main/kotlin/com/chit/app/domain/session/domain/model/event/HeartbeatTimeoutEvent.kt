package com.chit.app.domain.session.domain.model.event

data class HeartbeatTimeoutEvent(
        val sessionCode: String,
        val memberId: Long
)