package com.chit.app.domain.session.application.dto

enum class SseEvent {
    STREAMER_SSE_INITIALIZATION,
    SESSION_STATUS_UPDATED,
    PARTICIPANT_ADDED,
    PARTICIPANT_REMOVED,
    PARTICIPANT_UPDATED,
    SESSION_CLOSED
}