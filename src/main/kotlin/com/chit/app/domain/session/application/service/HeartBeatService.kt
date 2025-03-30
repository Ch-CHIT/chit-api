package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.sse.infrastructure.SseAdapter
import com.chit.app.domain.sse.infrastructure.SseEmitterManager
import com.chit.app.domain.sse.infrastructure.SseEventType
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@Service
class HeartBeatService(
        private val sseAdapter: SseAdapter,
        private val sseEmitterManager: SseEmitterManager,
        private val sessionService: SessionService,
        private val participantService: ParticipantService,
        private val sessionRepository: SessionRepository
) {
    
    private data class HeartbeatKey(val memberId: Long, val sessionCode: String)
    
    private val lastHeartbeats = ConcurrentHashMap<HeartbeatKey, Long>()
    private val HEARTBEAT_TIMEOUT_MS = 15_000L
    
    init {
        Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory()).scheduleAtFixedRate({
            val now = System.currentTimeMillis()
            lastHeartbeats.forEach { (key, lastTime) ->
                if (now - lastTime > HEARTBEAT_TIMEOUT_MS) {
                    processHeartbeatTimeout(key)
                }
            }
        }, HEARTBEAT_TIMEOUT_MS, HEARTBEAT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
    }
    
    fun touch(memberId: Long, sessionCode: String) {
        lastHeartbeats[HeartbeatKey(memberId, sessionCode)] = System.currentTimeMillis()
    }
    
    private fun processHeartbeatTimeout(key: HeartbeatKey) {
        val contentsSession = sessionRepository.findOpenContentsSessionBy(sessionCode = key.sessionCode) ?: return
        if (contentsSession.streamerId == key.memberId) {
            processStreamerTimeout(key)
        } else {
            processParticipantTimeout(key)
        }
        lastHeartbeats.remove(key)
    }
    
    private fun processStreamerTimeout(key: HeartbeatKey) {
        val sessionCode = sessionService.closeContentsSession(key.memberId)
        sseAdapter.broadcastEvent(sessionCode, SseEventType.CLOSED_SESSION, null)
        sseEmitterManager.unsubscribeAll(sessionCode)
    }
    
    private fun processParticipantTimeout(key: HeartbeatKey) {
        participantService.leaveSession(key.sessionCode, key.memberId)
    }
    
}