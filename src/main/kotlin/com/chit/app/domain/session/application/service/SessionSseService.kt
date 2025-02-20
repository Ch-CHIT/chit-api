package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.application.service.util.SseUtil
import com.chit.app.domain.session.domain.model.event.ParticipantDisconnectionEvent
import com.chit.app.domain.session.domain.model.event.ParticipantJoinEvent
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.annotation.LogExecutionTime
import com.chit.app.global.delegate.logger
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

@Service
class SessionSseService(
        private val taskExecutor: ExecutorService,
        private val eventPublisher: ApplicationEventPublisher
) {
    
    private val log = logger<SessionSseService>()
    
    companion object {
        private val viewerIdToSessionCode = ConcurrentHashMap<Long, String>()
        private val emitters = ConcurrentHashMap<String, ConcurrentHashMap<Long, SseEmitter>>()
    }
    
    fun subscribe(
            viewerId: Long,
            sessionCode: String,
            gameNickname: String
    ): SseEmitter {
        val existingSessionCode = viewerIdToSessionCode[viewerId]
        if (existingSessionCode != null && existingSessionCode != sessionCode) {
            disconnectParticipant(existingSessionCode, viewerId)
        }
        
        val emitter = emitters[sessionCode]?.get(viewerId) ?: createAndRegisterEmitter(sessionCode, viewerId)
        eventPublisher.publishEvent(ParticipantJoinEvent(sessionCode, viewerId, gameNickname))
        return emitter
    }
    
    @LogExecutionTime
    fun reorderSessionParticipants(sessionCode: String, sseEvent: SseEvent) {
        val participantEmitters = emitters[sessionCode] ?: return
        if (participantEmitters.isEmpty()) return
        
        val sortedParticipants = ParticipantOrderManager.getSortedParticipants(sessionCode)
        val futures = sortedParticipants.mapIndexed { index, participantOrder ->
            participantEmitters[participantOrder.viewerId]?.let { emitter ->
                runAsync({
                    emitParticipantEvent(
                        emitter = emitter,
                        sseEvent = sseEvent,
                        eventData = mapOf(
                            "order" to (index + 1),
                            "fixed" to participantOrder.fixed,
                            "status" to participantOrder.status,
                            "viewerId" to participantOrder.viewerId,
                            "participantId" to participantOrder.participantId
                        )
                    )
                }, taskExecutor)
            } ?: completedFuture(null)
        }
        
        allOf(*futures.toTypedArray()).join()
        log.debug("세션 $sessionCode 의 모든 참가자 순서 업데이트 완료.")
    }
    
    @LogExecutionTime
    fun disconnectParticipant(sessionCode: String, viewerId: Long) {
        val sessionEmitters = emitters[sessionCode] ?: return
        val emitter = sessionEmitters.remove(viewerId) ?: return
        
        try {
            emitter.complete()
        } catch (ex: Exception) {
            handleEmitterCompletionError(sessionCode, viewerId, ex)
        } finally {
            if (sessionEmitters.isEmpty()) {
                emitters.remove(sessionCode)
                log.debug("빈 세션 {} 정리 완료", sessionCode)
            }
            log.debug("참가자 제거 완료 - sessionCode: {}, viewerId: {}", sessionCode, viewerId)
        }
    }
    
    @LogExecutionTime
    fun disconnectAllParticipants(sessionCode: String) {
        val sessionEmitters = emitters.remove(sessionCode) ?: return
        val futures = sessionEmitters.map { (viewerId, emitter) ->
            runAsync({
                try {
                    emitter.complete()
                } catch (error: Exception) {
                    emitter.completeWithError(error)
                    log.error("사용자 {}의 SSE 연결 종료 중 오류 발생: {}", viewerId, error.message, error)
                }
            }, taskExecutor)
        }
        allOf(*futures.toTypedArray()).join()
        log.debug("세션 {}의 모든 emitters가 성공적으로 종료되었습니다.", sessionCode)
    }
    
    @LogExecutionTime
    fun clearAllParticipantEmitters() {
        val allParticipantEmitters = emitters.values.flatMap { it.values }
        val futures = allParticipantEmitters.map { emitter ->
            runAsync({
                try {
                    emitter.complete()
                } catch (error: Exception) {
                    emitter.completeWithError(error)
                    log.error("SSE 연결 종료 중 오류 발생: {}", error.message, error)
                }
            }, taskExecutor)
        }
        allOf(*futures.toTypedArray()).join()
        emitters.clear()
    }
    
    private fun emitParticipantEvent(emitter: SseEmitter, sseEvent: SseEvent, eventData: Map<String, Any>) {
        try {
            SseUtil.emitEvent(emitter, sseEvent, eventData)
        } catch (error: Exception) {
            try {
                emitter.completeWithError(error)
            } catch (completionError: Exception) {
                log.error("Emitter 종료 실패 - 이벤트: {}, 에러: {}", sseEvent.name, completionError.message, completionError)
            }
        }
    }
    
    private fun handleEmitterCompletionError(sessionCode: String, viewerId: Long, ex: Exception) {
        when (ex) {
            is IllegalStateException -> log.error(
                "Emitter가 이미 종료되어 complete() 호출을 건너뜁니다 - sessionCode: {}, viewerId: {}, 에러: {}",
                sessionCode, viewerId, ex.message, ex
            )
            
            else                     -> log.error(
                "Emitter 종료 중 오류 발생 - sessionCode: {}, viewerId: {}, 에러: {}",
                sessionCode, viewerId, ex.message, ex
            )
        }
    }
    
    private fun createAndRegisterEmitter(sessionCode: String, viewerId: Long): SseEmitter {
        val emitter = buildEmitterWithCleanup(sessionCode, viewerId)
        val sessionEmitters = emitters.computeIfAbsent(sessionCode) { ConcurrentHashMap() }
        sessionEmitters[viewerId] = emitter
        viewerIdToSessionCode[viewerId] = sessionCode
        log.debug("새로운 Emitter 등록됨 - sessionCode: {}, viewerId: {}", sessionCode, viewerId)
        return emitter
    }
    
    private fun buildEmitterWithCleanup(sessionCode: String, viewerId: Long): SseEmitter {
        val timeout = System.getenv("SSE_TIMEOUT")?.toLong() ?: (60 * 60 * 1000L)
        val cleanup: () -> Unit = {
            emitters[sessionCode]?.remove(viewerId)
            viewerIdToSessionCode.remove(viewerId)
            eventPublisher.publishEvent(ParticipantDisconnectionEvent(sessionCode, viewerId))
        }
        
        return SseEmitter(timeout).apply {
            onCompletion {
                cleanup()
                log.debug("Emitter << onCompletion >> - sessionCode: {}, viewerId: {}", sessionCode, viewerId)
            }
            onTimeout {
                cleanup()
                log.warn("Emitter << onTimeout >> - sessionCode: {}, viewerId: {}", sessionCode, viewerId)
            }
            onError { error ->
                cleanup()
                log.error("Emitter << onError >> - sessionCode: {}, viewerId: {}, 에러: {}", sessionCode, viewerId, error.message, error)
            }
        }
    }
}