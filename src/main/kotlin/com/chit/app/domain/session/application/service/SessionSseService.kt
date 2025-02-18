package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.event.ParticipantDisconnectionEvent
import com.chit.app.domain.session.domain.model.event.ParticipantJoinEvent
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.domain.session.application.service.util.SseUtil
import com.chit.app.global.delegate.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import kotlin.system.measureTimeMillis

@Service
class SessionSseService(
        private val taskExecutor: ExecutorService,
        private val eventPublisher: ApplicationEventPublisher
) {
    
    private val log = logger<SessionSseService>()
    
    private val viewerToSessionMap = ConcurrentHashMap<Long, String>()
    private val emitters = ConcurrentHashMap<String, ConcurrentHashMap<Long, SseEmitter>>()
    
    fun subscribe(viewerId: Long, sessionCode: String, gameNickname: String): SseEmitter {
        // 1) 기존 세션 연결 체크
        val existingSessionCode = viewerToSessionMap[viewerId]
        
        // 2) 다른 세션에 연결되어 있다면 정리
        if (existingSessionCode != null && existingSessionCode != sessionCode) {
            disconnectParticipant(existingSessionCode, viewerId)
        }
        
        // 3) emitter 변수에 기존 emitter가 있으면 사용, 없으면 새로 생성
        val emitter = emitters[sessionCode]?.get(viewerId) ?: createAndRegisterEmitter(sessionCode, viewerId)
        
        // 4) 가입 이벤트 발행
        eventPublisher.publishEvent(ParticipantJoinEvent(sessionCode, viewerId, gameNickname))
        
        return emitter
    }
    
    fun reorderSessionParticipants(sessionCode: String, sseEvent: SseEvent) {
        val participantEmitters = emitters[sessionCode] ?: return
        if (participantEmitters.isEmpty()) return
        
        val sortedParticipants = ParticipantOrderManager.getSortedParticipants(sessionCode)
        val timeTaken = measureTimeMillis {
            val futures = sortedParticipants.mapIndexed { index, participantOrder ->
                participantEmitters[participantOrder.viewerId]?.let { emitter ->
                    CompletableFuture.runAsync({
                        try {
                            SseUtil.emitEvent(
                                emitter, sseEvent, mapOf(
                                    "order" to (index + 1),
                                    "fixed" to participantOrder.fixed,
                                    "status" to participantOrder.status,
                                    "viewerId" to participantOrder.viewerId,
                                    "participantId" to participantOrder.participantId,
                                )
                            )
                        } catch (error: Exception) {
                            try {
                                emitter.completeWithError(error)
                            } catch (completionError: Exception) {
                                log.error("Emitter 종료 실패 - 이벤트: {}, 에러: {}", sseEvent.name, completionError.message, completionError)
                            }
                        }
                    }, taskExecutor)
                } ?: CompletableFuture.completedFuture(null)
            }
            CompletableFuture.allOf(*futures.toTypedArray()).join()
        }
        log.debug("세션 $sessionCode 의 모든 참가자 순서 업데이트 완료. 소요 시간: $timeTaken ms")
    }
    
    fun disconnectParticipant(sessionCode: String, viewerId: Long) {
        val sessionEmitters = emitters[sessionCode] ?: return
        val emitter = sessionEmitters.remove(viewerId) ?: return
        try {
            emitter.complete()
        } catch (ex: IllegalStateException) {
            log.warn("Emitter가 이미 종료되어 complete() 호출을 건너뜁니다 - sessionCode: {}, viewerId: {}", sessionCode, viewerId)
        } catch (ex: Exception) {
            log.error("Emitter 종료 중 오류 발생 - sessionCode: {}, viewerId: {}, 에러: {}", sessionCode, viewerId, ex.message, ex)
        } finally {
            if (sessionEmitters.isEmpty()) {
                emitters.remove(sessionCode)
                log.debug("빈 세션 {} 정리 완료", sessionCode)
            }
            log.debug("참가자 제거 완료 - sessionCode: {}, viewerId: {}", sessionCode, viewerId)
        }
    }
    
    fun disconnectAllParticipants(sessionCode: String) {
        val sessionEmitters = emitters.remove(sessionCode) ?: return
        val timeTaken = measureTimeMillis {
            val futures = sessionEmitters.map { (viewerId, emitter) ->
                CompletableFuture.runAsync({
                    try {
                        emitter.complete()
                    } catch (error: Exception) {
                        emitter.completeWithError(error)
                        log.error("사용자 {}의 SSE 연결 종료 중 오류 발생: {}", viewerId, error.message, error)
                    }
                }, taskExecutor)
            }
            CompletableFuture.allOf(*futures.toTypedArray()).join()
        }
        log.debug("세션 {}의 모든 emitters가 성공적으로 종료되었습니다. 소요 시간: {} ms", sessionCode, timeTaken)
    }
    
    fun clearAllParticipantEmitters() {
        val allParticipantEmitters = emitters.values.flatMap { it.values }
        val futures = allParticipantEmitters.map { emitter ->
            CompletableFuture.runAsync({
                try {
                    emitter.complete()
                } catch (error: Exception) {
                    emitter.completeWithError(error)
                    log.error("SSE 연결 종료 중 오류가 발생했습니다: {}", error.message, error)
                }
            }, taskExecutor)
        }
        
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        emitters.clear()
    }
    
    private fun createAndRegisterEmitter(sessionCode: String, viewerId: Long): SseEmitter {
        val timeout = System.getenv("SSE_TIMEOUT")?.toLong() ?: (60 * 60 * 1000L)
        val emitter = SseEmitter(timeout).apply {
            fun cleanup() {
                emitters[sessionCode]?.remove(viewerId)
                viewerToSessionMap.remove(viewerId)
                eventPublisher.publishEvent(ParticipantDisconnectionEvent(sessionCode, viewerId))
            }
            
            onCompletion {
                cleanup()
                log.debug("Emitter onCompletion 호출됨 - sessionCode: {}, viewerId: {}", sessionCode, viewerId)
            }
            
            onTimeout {
                cleanup()
                log.warn("Emitter onTimeout 발생 - sessionCode: {}, viewerId: {}", sessionCode, viewerId)
            }
            
            onError { error ->
                cleanup()
                log.error("Emitter onError 발생 - sessionCode: {}, viewerId: {}, 에러: {}", sessionCode, viewerId, error.message, error)
            }
            
        }
        
        emitters.computeIfAbsent(sessionCode) { ConcurrentHashMap() }[viewerId] = emitter
        viewerToSessionMap[viewerId] = sessionCode
        log.debug("새로운 Emitter 등록됨 - sessionCode: {}, viewerId: {}", sessionCode, viewerId)
        
        return emitter
    }
    
}
