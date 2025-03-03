package com.chit.app.domain.session.application.dto

enum class SseEvent {
    STREAMER_SESSION_UPDATED,
    STREAMER_PARTICIPANT_FIXED,
    STREAMER_PARTICIPANT_ADDED,
    STREAMER_SSE_INITIALIZATION,
    STREAMER_PARTICIPANT_REMOVED,
    
    PARTICIPANT_ORDER_UPDATED,
    PARTICIPANT_SESSION_UPDATED,
    PARTICIPANT_SESSION_CLOSED,
    PARTICIPANT_SESSION_KICKED
}