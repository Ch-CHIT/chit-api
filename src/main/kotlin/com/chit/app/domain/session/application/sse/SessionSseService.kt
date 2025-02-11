package com.chit.app.domain.session.application.sse

import com.chit.app.domain.session.application.ParticipantService
import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.application.event.ParticipantDisconnectionEvent
import com.chit.app.domain.session.application.sse.SseUtil.completeAllEmitters
import com.chit.app.domain.session.application.sse.SseUtil.createSseEmitter
import com.chit.app.domain.session.application.sse.SseUtil.emitEvent
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.global.delegate.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.ConcurrentHashMap
import kotlin.system.measureTimeMillis

@Service
class SessionSseService(
        private val participantService: ParticipantService,
        private val sessionRepository: SessionRepository,
        private val eventPublisher: ApplicationEventPublisher,
) {
    
    private val log = logger<SessionSseService>()
    private val sseTimeout = System.getenv("SSE_TIMEOUT")?.toLong() ?: (60 * 60 * 1000L)
    
    private val participantSessionMap = ConcurrentHashMap<Long, String>()
    private val emitters = ConcurrentHashMap<String, ConcurrentHashMap<Long, SseEmitter>>()
    
    @Transactional
    suspend fun subscribe(participantId: Long, sessionCode: String, gameNickname: String): SseEmitter {
        val emitter = initializeEmitter(sessionCode, participantId)
        participantService.joinSession(sessionCode, participantId, gameNickname)
        reorderSessionParticipants(sessionCode, SseEvent.PARTICIPANT_ADDED)
        return emitter
    }
    
    fun disconnectParticipant(sessionCode: String, participantId: Long) {
        val sessionEmitters = emitters[sessionCode] ?: return
        val emitter = sessionEmitters.remove(participantId) ?: return
        try {
            emitter.complete()
            participantSessionMap.remove(participantId)
            if (sessionEmitters.isEmpty()) {
                emitters.remove(sessionCode)
                log.info("빈 세션 {} 정리 완료", sessionCode)
            }
            log.info("참가자 제거 완료 - 세션: {}, 참가자: {}", sessionCode, participantId)
        } catch (error: Exception) {
            log.error("참가자 {}의 연결 종료 중 오류 발생: {}", participantId, error.message, error)
        }
    }
    
    suspend fun disconnectAllParticipants(sessionCode: String) {
        val sessionEmitters = emitters.remove(sessionCode) ?: return
        val timeTaken = measureTimeMillis {
            coroutineScope {
                sessionEmitters.forEach { (participantId, emitter) ->
                    launch(Dispatchers.IO) {
                        try {
                            emitEvent(emitter, SseEvent.SESSION_CLOSED, "세션이 종료되었습니다.")
                            emitter.complete()
                            log.info("사용자 {}의 SSE 연결이 성공적으로 종료되었습니다.", participantId)
                        } catch (error: Exception) {
                            log.error("사용자 {}의 SSE 연결 종료 중 오류 발생: {}", participantId, error.message, error)
                        }
                    }
                }
            }
        }
        log.info("파티 {}의 모든 emitters가 성공적으로 종료되었습니다. 소요 시간: {} ms", sessionCode, timeTaken)
    }
    
    suspend fun clearAllParticipantEmitters() =
            completeAllEmitters(
                emitters = emitters.entries.flatMap { (_, participantEmitters) -> participantEmitters.values },
                onFailure = { error -> log.error("SSE 연결 종료 중 오류가 발생했습니다: {}", error.message) }
            ).also {
                emitters.clear()
            }
    
    suspend fun reorderSessionParticipants(sessionCode: String, sseEvent: SseEvent) {
        val sessionEmitters = emitters[sessionCode] ?: return
        if (sessionEmitters.isEmpty()) {
            return
        }
        
        val participants = sessionRepository.findSortedParticipantsBySessionCode(sessionCode)
        val participantEmitters = emitters[sessionCode] ?: return
        val timeTaken = measureTimeMillis {
            coroutineScope {
                participants.mapIndexed { index, participant ->
                    launch(Dispatchers.IO) {
                        participantEmitters.get(participant.participantId)?.let { emitter ->
                            try {
                                emitEvent(
                                    emitter, sseEvent,
                                    mapOf(
                                        "order" to index + 1,
                                        "fixed" to participant.fixedPick
                                    )
                                )
                            } catch (error: Exception) {
                                try {
                                    emitter.completeWithError(error)
                                } catch (completionError: Exception) {
                                    log.error("Emitter 종료 실패 - 이벤트: {}, 에러: {}", sseEvent.name, completionError.message, completionError)
                                }
                            }
                        }
                    }
                }
            }
        }
        log.debug("세션 ${sessionCode}의 모든 참가자 순서 업데이트 완료. 소요 시간: ${timeTaken} ms")
    }
    
    private fun initializeEmitter(sessionCode: String, participantId: Long): SseEmitter {
        cleanupExistingConnection(sessionCode, participantId)
        val emitter = createSseEmitter(
            timeout = sseTimeout,
            onCompletion = { handleDisconnection(sessionCode, participantId) },
            onTimeout = { handleDisconnection(sessionCode, participantId) },
            onError = { error -> handleDisconnection(sessionCode, participantId) }
        )
        registerEmitter(sessionCode, participantId, emitter)
        return emitter
    }
    
    private fun cleanupExistingConnection(sessionCode: String, participantId: Long) {
        participantSessionMap[participantId]?.let { existingSessionCode ->
            if (existingSessionCode != sessionCode) {
                disconnectParticipant(existingSessionCode, participantId)
            }
        }
    }
    
    private fun registerEmitter(
            sessionCode: String,
            participantId: Long,
            emitter: SseEmitter
    ) {
        val sessionEmitters = emitters.computeIfAbsent(sessionCode) { ConcurrentHashMap() }
        sessionEmitters[participantId] = emitter
        participantSessionMap[participantId] = sessionCode
    }
    
    private fun handleDisconnection(sessionCode: String, participantId: Long) {
        disconnectParticipant(sessionCode, participantId)
        eventPublisher.publishEvent(ParticipantDisconnectionEvent(sessionCode, participantId))
    }
    
}
