package com.chit.app.domain.sse.infrastructure

import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture.allOf
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

@Component
class SseEmitterManager(
        private val taskExecutor: ExecutorService
) {
    
    private val log = logger<SseEmitterManager>()
    private val memberIdToSessionCode = ConcurrentHashMap<Long, String>()
    private val emitters = ConcurrentHashMap<String, ConcurrentHashMap<Long, SseEmitter>>()
    
    fun getEmitter(sessionCode: String, memberId: Long): SseEmitter? = getEmittersBySession(sessionCode)?.get(memberId)
    fun getEmittersBySession(sessionCode: String): ConcurrentHashMap<Long, SseEmitter>? = emitters[sessionCode]
    
    fun subscribe(memberId: Long, sessionCode: String): SseEmitter {
        memberIdToSessionCode[memberId]?.let { existingSessionCode ->
            if (existingSessionCode != sessionCode) {
                emitters[existingSessionCode]?.remove(memberId)?.complete()
                log.info("기존 SSE 연결 종료됨. (회원 ID: $memberId, 세션코드: $existingSessionCode)")
            }
        }
        
        val emitter = createNewEmitter(memberId, sessionCode)
        emitters.getOrPut(sessionCode) { ConcurrentHashMap() }[memberId] = emitter
        memberIdToSessionCode[memberId] = sessionCode
        return emitter
    }
    
    fun unsubscribe(sessionCode: String, memberId: Long) {
        getEmitter(sessionCode, memberId)?.complete()
    }
    
    fun unsubscribeAll(sessionCode: String) {
        val sessionEmitters = getEmittersBySession(sessionCode) ?: return
        sessionEmitters.forEach { (_, emitter) -> emitter.complete() }
    }
    
    fun completeAllEmitters() {
        val futures = emitters.values.flatMap { sessionEmitters ->
            sessionEmitters.entries.map { (memberId, emitter) ->
                runAsync({
                    try {
                        emitter.complete()
                        log.info("SSE 연결 종료 완료: 회원 ID: $memberId, 세션코드: ${sessionEmitters.keys.firstOrNull()}")
                    } catch (e: Exception) {
                        log.error("SSE 연결 종료 실패: 회원 ID: $memberId, 세션코드: ${sessionEmitters.keys.firstOrNull()}, 에러: ${e.message}", e)
                    }
                }, taskExecutor)
            }
        }
        
        allOf(*futures.toTypedArray()).join()
        emitters.clear()
        memberIdToSessionCode.clear()
    }
    
    private fun createNewEmitter(memberId: Long, sessionCode: String): SseEmitter {
        val cleanup: (Long, String) -> Unit = { memberId, sessionCode ->
            emitters[sessionCode]?.remove(memberId)
            memberIdToSessionCode.remove(memberId)
        }
        
        return SseEmitter(Long.MAX_VALUE).apply {
            onCompletion {
                cleanup(memberId, sessionCode)
                log.info("Emitter가 완료되었습니다. (회원 ID: $memberId, 세션코드: $sessionCode)")
            }
            onTimeout {
                cleanup(memberId, sessionCode)
                log.info("Emitter 타임아웃 발생하였습니다. (회원 ID: $memberId, 세션코드: $sessionCode)")
            }
            onError { e ->
                cleanup(memberId, sessionCode)
                log.error("Emitter 오류 발생 (회원 ID: $memberId, 세션코드: $sessionCode): ${e.message}", e)
            }
        }
    }
    
}