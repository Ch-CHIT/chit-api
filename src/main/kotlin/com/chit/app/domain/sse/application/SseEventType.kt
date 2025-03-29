package com.chit.app.domain.sse.application

enum class SseEventType(val message: String) {
    
    SESSION_JOINED("세션 참가 완료"),
    
    STREAMER_SESSION_UPDATED("스트리머 세션 업데이트"),
    STREAMER_PARTICIPANT_FIXED("스트리머 참여자 고정됨"),
    STREAMER_PARTICIPANT_ADDED("스트리머 참여자 추가됨"),
    STREAMER_SSE_INITIALIZATION("스트리머 SSE 초기화"),
    STREAMER_SSE_DISCONNECT("스트리머 SSE 연결 해제"),
    STREAMER_PARTICIPANT_REMOVED("스트리머 참여자 제거됨"),
    
    PARTICIPANT_ORDER_UPDATED("참여자 순서 업데이트"),
    PARTICIPANT_SESSION_UPDATED("참여자 세션 업데이트"),
    PARTICIPANT_SESSION_CLOSED("참여자 세션 종료"),
    PARTICIPANT_SESSION_KICKED("참여자 세션 추방됨")
    
}