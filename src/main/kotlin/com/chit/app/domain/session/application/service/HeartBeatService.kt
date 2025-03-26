package com.chit.app.domain.session.application.service

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class HeartBeatService(
        private val sessionSseService: SessionSseService,
        private val streamerSseService: StreamerSseService
) {
    
    private val lastHeartbeats = ConcurrentHashMap<Long, Long>()
    private val sessionCodes = ConcurrentHashMap<Long, String>()
    private val HEARTBEAT_TIMEOUT_MS = 15_000L
    
    init {
        Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory()).scheduleAtFixedRate({
            val now = System.currentTimeMillis()
            lastHeartbeats.forEach { (memberId, lastTime) ->
                if (now - lastTime > HEARTBEAT_TIMEOUT_MS) {
                    val sessionCode = sessionCodes.remove(memberId)
                    if (sessionCode != null) {
                        sessionSseService.disconnectSseEmitter(sessionCode, memberId)
                    } else {
                        streamerSseService.unsubscribe(memberId)
                    }
                    lastHeartbeats.remove(memberId)
                }
            }
        }, HEARTBEAT_TIMEOUT_MS, HEARTBEAT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }
    
    fun touchViewer(memberId: Long, sessionCode: String) {
        lastHeartbeats[memberId] = System.currentTimeMillis()
        sessionCodes[memberId] = sessionCode
    }
    
    fun touchStreamer(memberId: Long) {
        lastHeartbeats[memberId] = System.currentTimeMillis()
    }
    
}