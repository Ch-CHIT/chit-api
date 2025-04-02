package com.chit.app.domain.sse.infrastructure

enum class SseEventType(val message: String) {
    
    JOINED_SESSION("세션에 참가하셨습니다."),
    CLOSED_SESSION("세션이 종료되었습니다."),
    LEFT_SESSION("세션에서 퇴장했습니다."),
    KICKED_SESSION("세션에서 강제 퇴장되었습니다."),
    UPDATED_SESSION("세션 정보가 업데이트되었습니다."),
    SESSION_ORDER_UPDATED("세션 내 참여자 순서가 변경되었습니다."),
    PARTICIPANT_JOINED_SESSION("새로운 참여자가 입장하였습니다."),
    PARTICIPANT_LEFT_SESSION("참여자가 세션에서 퇴장하였습니다."),
    PARTICIPANT_KICKED_SESSION("참여자가 세션에서 강제 퇴장되었습니다."),
    PARTICIPANT_FIXED_SESSION("스트리머가 해당 참여자를 고정하였습니다."),
    
}