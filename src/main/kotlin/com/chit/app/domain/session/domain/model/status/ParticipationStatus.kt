package com.chit.app.domain.session.domain.model.status

enum class ParticipationStatus(val order: Int) {
    // 게임 참여 중 상태가 가장 우선
    APPROVED(1),
    
    // 참가 대기 상태가 그 다음
    PENDING(2),
    
    // 게임을 나간 상태
    LEFT(3),
    
    // 추방 상태
    REJECTED(4);
}