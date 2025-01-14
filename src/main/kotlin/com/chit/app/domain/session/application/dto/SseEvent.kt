package com.chit.app.domain.session.application.dto

enum class SseEvent {
    SESSION_STATUS_UPDATED,
    SESSION_INFORMATION_UPDATED,
    PARTICIPANT_ADDED,
    PARTICIPANT_REMOVED,
    PARTICIPANT_UPDATED,
    SESSION_CLOSED
}