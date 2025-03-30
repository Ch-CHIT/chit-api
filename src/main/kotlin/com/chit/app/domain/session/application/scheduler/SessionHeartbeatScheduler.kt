package com.chit.app.domain.session.application.scheduler

import com.chit.app.domain.session.application.service.HeartBeatService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class SessionHeartbeatScheduler(
    private val heartBeatService: HeartBeatService
) {
    
    private data class HeartbeatKey(val memberId: Long, val sessionCode: String)
    
    private val lastHeartbeats = ConcurrentHashMap<HeartbeatKey, Long>()
    private val HEARTBEAT_TIMEOUT_MS = 15_000L
    
    @Scheduled(fixedDelay = 15000, initialDelay = 15000)
    fun checkHeartbeats() {
        val now = System.currentTimeMillis()
        lastHeartbeats.forEach { (key, lastTime) ->
            if (now - lastTime > HEARTBEAT_TIMEOUT_MS) {
                heartBeatService.processHeartbeatTimeout(key.sessionCode, key.memberId)
                lastHeartbeats.remove(key)
            }
        }
    }
    
    fun touch(memberId: Long, sessionCode: String) {
        lastHeartbeats[HeartbeatKey(memberId, sessionCode)] = System.currentTimeMillis()
    }
    
}