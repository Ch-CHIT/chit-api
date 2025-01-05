package com.chit.app.domain.auth.domain.model

import java.time.Instant
import java.time.temporal.ChronoUnit

data class RefreshToken(
        val token: String,
        val memberId: Long,
        val expiration: Instant = Instant.now().plus(1, ChronoUnit.DAYS)
) {
    
    init {
        require(token.isNotBlank()) { "토큰 값은 비어있을 수 없습니다." }
        require(memberId > 0) { "회원 ID는 0보다 커야 합니다." }
    }
    
    fun isExpired(): Boolean = Instant.now().isAfter(expiration)
    
}