package com.chit.app.domain.session.application.scheduler

import com.chit.app.domain.session.domain.model.event.HeartbeatTimeoutEvent
import com.chit.app.global.common.logging.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class SessionHeartbeatScheduler(
        private val applicationEventPublisher: ApplicationEventPublisher
) {
    private data class HeartbeatKey(val memberId: Long, val sessionCode: String)
    
    private val log = logger<SessionHeartbeatScheduler>()
    private val lastHeartbeats = ConcurrentHashMap<HeartbeatKey, Long>()
    
    companion object {
        /** 하트비트 타임아웃 기준 (ms) */
        const val HEARTBEAT_TIMEOUT_MS = 15_000L
        
        /** 하트비트 체크 주기 (ms) */
        const val CHECK_INTERVAL_MS = 5_000L
    }
    
    @Scheduled(fixedDelay = CHECK_INTERVAL_MS)
    fun checkHeartbeats() {
        val now = System.currentTimeMillis()
        val expiredKeys = mutableListOf<HeartbeatKey>()
        
        lastHeartbeats.forEach { (key, lastTime) ->
            if (now - lastTime > HEARTBEAT_TIMEOUT_MS) {
                val (memberId, sessionCode) = key
                expiredKeys.add(key)
                applicationEventPublisher.publishEvent(HeartbeatTimeoutEvent(key.sessionCode, key.memberId))
                log.info("[알림] 하트비트 타임아웃: sessionCode={}, memberId={}", sessionCode, memberId)
            }
        }
        
        expiredKeys.forEach { lastHeartbeats.remove(it) }
    }
    
    fun touch(memberId: Long, sessionCode: String) {
        lastHeartbeats[HeartbeatKey(memberId, sessionCode)] = System.currentTimeMillis()
    }
    
    
}