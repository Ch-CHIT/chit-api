package com.chit.app.domain.session.application.sse

import com.chit.app.domain.session.application.ParticipantService
import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.SessionParticipant
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.global.delegate.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

@Service
class SessionSseService(
        private val executor: ExecutorService,
        private val participantService: ParticipantService,
        private val sessionRepository: SessionRepository
) {
    
    private val log = logger<SessionSseService>()
    private val sseTimeout = System.getenv("SSE_TIMEOUT")?.toLong() ?: (60 * 60 * 1000L)
    
    private val participantSessionMap = ConcurrentHashMap<Long, String>()
    private val emitters = ConcurrentHashMap<String, ConcurrentHashMap<Long, SseEmitter>>()
    
    @Transactional
    fun subscribe(participantId: Long, sessionCode: String, gameNickname: String): SseEmitter {
        val emitter = initializeEmitter(sessionCode, participantId)
        participantService.joinSession(sessionCode, participantId, gameNickname)
        updateAllParticipantsOrder(sessionCode, SseEvent.PARTICIPANT_ADDED)
        return emitter
    }
    
    fun disconnectParticipant(sessionCode: String, participantId: Long) {
        val sessionEmitters = emitters[sessionCode]
                ?: run {
                    log.warn("세션 {}이 존재하지 않습니다.", sessionCode)
                    return
                }
        
        val emitter = sessionEmitters.remove(participantId)
        if (emitter == null) {
            log.warn("세션 {}에서 참가자 {}의 이미터를 찾을 수 없습니다.", sessionCode, participantId)
            return
        }
        
        completeEmitter(emitter, participantId)
        participantSessionMap.remove(participantId)
        checkAndCleanupEmptySession(sessionCode, sessionEmitters)
        
        log.info("참가자 제거 완료 - 세션: {}, 참가자: {}", sessionCode, participantId)
    }
    
    fun removeAllParticipants(sessionCode: String) {
        val sessionEmitters = emitters.remove(sessionCode) ?: return
        val futures = sessionEmitters.map { (participantId, emitter) ->
            CompletableFuture.runAsync({
                runCatching {
                    emitEvent(emitter, SseEvent.SESSION_CLOSED, "세션이 종료되었습니다.")
                    emitter.complete()
                }.onFailure { error ->
                    log.error("사용자 {}의 SSE 연결 종료 중 오류 발생: {}", participantId, error.message)
                }
            }, executor)
        }
        
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        log.info("파티 {}의 모든 emitters가 성공적으로 종료되었습니다.", sessionCode)
    }
    
    fun clearAllSessions() {
        val futures = emitters.entries.flatMap { (sessionCode, participantEmitters) ->
            participantEmitters.map { (participantId, emitter) ->
                CompletableFuture.runAsync({
                    runCatching {
                        emitter.complete()
                    }.onFailure { error ->
                        log.error("사용자 {}의 파티 {} SSE 연결 종료 중 오류 발생: {}", participantId, sessionCode, error.message)
                    }
                }, executor)
            }
        }
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        emitters.clear()
    }
    
    fun updateAllParticipantsOrder(sessionCode: String, sseEvent: SseEvent) {
        val sessionEmitters = emitters[sessionCode] ?: return
        if (sessionEmitters.isEmpty()) {
            log.debug("세션 ${sessionCode}에 활성화된 이미터가 없습니다")
            return
        }
        
        val participants = sessionRepository.findSortedParticipantsBySessionCode(sessionCode)
        val futures = participants.mapIndexed { index, participant ->
            CompletableFuture.runAsync({
                sendQueuePosition(
                    order = index + 1,
                    sseEvent = sseEvent,
                    sessionCode = sessionCode,
                    participant = participant
                )
            }, executor)
                    .exceptionally { error ->
                        log.error("참가자 순서 업데이트 실패 - 세션: {}, 참가자: {}, 에러: {}", sessionCode, participant.id, error.message, error)
                        null
                    }
        }
        
        CompletableFuture.allOf(*futures.toTypedArray())
                .thenRun {
                    log.debug("세션 ${sessionCode}의 모든 참가자 순서 업데이트 완료")
                }
                .exceptionally { error ->
                    log.error("일부 참가자 순서 업데이트 실패 - 세션: {}", sessionCode, error)
                    null
                }
    }
    
    private fun completeEmitter(emitter: SseEmitter, participantId: Long) {
        runCatching {
            emitter.complete()
        }.onFailure { error ->
            log.error("참가자 {}의 연결 종료 중 오류 발생: {}", participantId, error.message, error)
        }
    }
    
    private fun checkAndCleanupEmptySession(sessionCode: String, sessionEmitters: ConcurrentHashMap<Long, SseEmitter>) {
        if (sessionEmitters.isEmpty()) {
            emitters.remove(sessionCode)
            log.info("빈 세션 {} 정리 완료", sessionCode)
        }
    }
    
    private fun sendQueuePosition(sessionCode: String, sseEvent: SseEvent, order: Int, participant: SessionParticipant) {
        emitters[sessionCode]
                ?.get(participant.participantId)
                ?.let { emitter -> sendSseEvent(emitter, sseEvent, order) }
    }
    
    private fun sendSseEvent(emitter: SseEmitter, event: SseEvent, data: Any) {
        try {
            emitEvent(emitter, event, data)
        } catch (error: Exception) {
            handleEmitterError(emitter, event, error)
        }
    }
    
    private fun emitEvent(emitter: SseEmitter, event: SseEvent, data: Any) {
        emitter.send(
            SseEmitter.event()
                    .name(event.name)
                    .data(data)
        )
    }
    
    private fun handleEmitterError(emitter: SseEmitter, event: SseEvent, error: Exception) {
        log.error(
            "이벤트 전송 실패 - 이벤트: {}, 에러: {}",
            event.name,
            error.message,
            error
        )
        
        try {
            emitter.completeWithError(error)
        } catch (completionError: Exception) {
            log.error(
                "Emitter 종료 실패 - 이벤트: {}, 에러: {}",
                event.name,
                completionError.message,
                completionError
            )
        }
    }
    
    private fun initializeEmitter(sessionCode: String, participantId: Long): SseEmitter {
        cleanupExistingConnection(sessionCode, participantId)
        val emitter = createEmitter(sessionCode, participantId)
        registerEmitter(sessionCode, participantId, emitter)
        return emitter
    }
    
    private fun cleanupExistingConnection(sessionCode: String, participantId: Long) {
        participantSessionMap[participantId]
                ?.let { existingSessionCode ->
                    if (existingSessionCode != sessionCode) {
                        log.debug("참가자 {}의 이전 세션 {} 연결 정리", participantId, existingSessionCode)
                        disconnectParticipant(existingSessionCode, participantId)
                    }
                }
    }
    
    private fun createEmitter(sessionCode: String, participantId: Long): SseEmitter {
        return SseEmitter(sseTimeout)
                .apply {
                    onCompletion {
                        log.debug("참가자 {}의 세션 {} 연결 완료", participantId, sessionCode)
                        disconnectParticipant(sessionCode, participantId)
                    }
                    
                    onTimeout {
                        log.debug("참가자 {}의 세션 {} 연결 타임아웃", participantId, sessionCode)
                        disconnectParticipant(sessionCode, participantId)
                    }
                    
                    onError { error ->
                        log.error(
                            "참가자 {}의 세션 {} SSE 연결 오류: {}",
                            participantId,
                            sessionCode,
                            error.message,
                            error
                        )
                        disconnectParticipant(sessionCode, participantId)
                    }
                }
    }
    
    private fun registerEmitter(sessionCode: String, participantId: Long, emitter: SseEmitter) {
        val sessionEmitters = emitters.computeIfAbsent(sessionCode) { ConcurrentHashMap() }
        sessionEmitters[participantId] = emitter
        participantSessionMap[participantId] = sessionCode
    }
}
