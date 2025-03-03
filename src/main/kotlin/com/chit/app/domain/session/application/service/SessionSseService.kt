package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.application.service.util.SseUtil
import com.chit.app.domain.session.domain.model.ParticipantOrder
import com.chit.app.domain.session.domain.model.event.ParticipantDisconnectionEvent
import com.chit.app.domain.session.domain.model.event.ParticipantJoinEvent
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.annotation.LogExecutionTime
import com.chit.app.global.common.logging.logger
import com.fasterxml.jackson.annotation.JsonInclude
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
            disconnectSseEmitter(existingSessionCode, viewerId)
        }
        
        val emitter = emitters[sessionCode]?.get(viewerId) ?: createAndRegisterEmitter(sessionCode, viewerId)
        eventPublisher.publishEvent(ParticipantJoinEvent(sessionCode, viewerId, gameNickname))
        return emitter
    }
    
    @LogExecutionTime
    fun reorderSessionParticipants(
            sseEvent: SseEvent,
            sessionCode: String,
            gameParticipationCode: String?,
            maxGroupParticipants: Int
    ) {
        val sessionEmitters = emitters[sessionCode] ?: return
        val sortedParticipants = ParticipantOrderManager.getSortedParticipantOrders(sessionCode)
        val futures = sortedParticipants.mapIndexed { index, participantOrder ->
            sessionEmitters[participantOrder.viewerId]?.let { emitter ->
                runAsync({
                    SseUtil.emitEvent(
                        sseEvent,
                        emitter,
                        ParticipantOrderEvent.of(index, participantOrder, gameParticipationCode, maxGroupParticipants)
                    )
                }, taskExecutor)
            } ?: completedFuture(null)
        }
        
        allOf(*futures.toTypedArray()).join()
    }
    
    @LogExecutionTime
    fun disconnectSseEmitter(sessionCode: String, viewerId: Long) {
        val sessionEmitters = emitters[sessionCode] ?: return
        val emitter = sessionEmitters.remove(viewerId) ?: return
        cleanupAndCompleteEmitter(sessionEmitters, emitter, sessionCode)
    }
    
    @LogExecutionTime
    fun disconnectAllSseEmitter(sessionCode: String) {
        val sessionEmitters = emitters.remove(sessionCode) ?: return
        val futures = sessionEmitters.map { (_, emitter) ->
            runAsync({
                cleanupAndCompleteEmitter(sessionEmitters, emitter, sessionCode)
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
                } catch (ex: Exception) {
                    emitter.completeWithError(ex)
                    log.error("Emitter 종료 중 오류 발생 : {}", ex.message, ex)
                }
            }, taskExecutor)
        }
        allOf(*futures.toTypedArray()).join()
        emitters.clear()
    }
    
    @LogExecutionTime
    fun emitSessionCloseEvent(sessionCode: String, viewerId: Long) {
        val sessionEmitters = emitters[sessionCode] ?: return
        val emitter = sessionEmitters.remove(viewerId) ?: return
        SseUtil.emitEvent(
            SseEvent.PARTICIPANT_SESSION_CLOSED,
            emitter,
            mapOf(
                "status" to "OK",
                "message" to "시참 세션이 종료되었습니다."
            )
        )
        cleanupAndCompleteEmitter(sessionEmitters, emitter, sessionCode)
    }
    
    private fun cleanupAndCompleteEmitter(
            sessionEmitters: ConcurrentHashMap<Long, SseEmitter>,
            emitter: SseEmitter,
            sessionCode: String
    ) {
        try {
            emitter.complete()
            if (sessionEmitters.isEmpty()) {
                emitters.remove(sessionCode)
            }
        } catch (ex: Exception) {
            emitter.completeWithError(ex)
            log.error("Emitter 종료 중 오류 발생 : {}", ex.message, ex)
        }
    }
    
    private fun createAndRegisterEmitter(sessionCode: String, viewerId: Long): SseEmitter {
        val timeout = System.getenv("SSE_TIMEOUT")?.toLong() ?: (60 * 60 * 1000L)
        val cleanup: () -> Unit = {
            emitters[sessionCode]?.remove(viewerId)
            viewerIdToSessionCode.remove(viewerId)
            eventPublisher.publishEvent(ParticipantDisconnectionEvent(sessionCode, viewerId))
        }
        
        val emitter = SseEmitter(timeout).apply {
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
        
        emitters.computeIfAbsent(sessionCode) { ConcurrentHashMap() }[viewerId] = emitter
        viewerIdToSessionCode[viewerId] = sessionCode
        return emitter
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class ParticipantOrderEvent(
            val order: Int,
            val fixed: Boolean,
            val status: ParticipationStatus,
            val viewerId: Long,
            val participantId: Long,
            val gameParticipationCode: String? = null
    ) {
        companion object {
            fun of(
                    index: Int,
                    participantOrder: ParticipantOrder,
                    gameParticipationCode: String?,
                    maxGroupParticipants: Int
            ): ParticipantOrderEvent {
                return ParticipantOrderEvent(
                    order = index + 1,
                    fixed = participantOrder.fixed,
                    status = participantOrder.status,
                    viewerId = participantOrder.viewerId,
                    participantId = participantOrder.participantId,
                    gameParticipationCode = gameParticipationCode?.takeIf { index + 1 <= maxGroupParticipants && it.isNotEmpty() }
                )
            }
        }
    }
    
}
