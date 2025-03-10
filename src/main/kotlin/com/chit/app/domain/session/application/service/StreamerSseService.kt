package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.application.service.util.SseUtil
import com.chit.app.global.annotation.LogExecutionTime
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture.allOf
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

@Service
class StreamerSseService(
        private val taskExecutor: ExecutorService
) {
    
    private val log = logger<StreamerSseService>()
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()
    
    fun subscribe(streamerId: Long): SseEmitter {
        emitters[streamerId]?.apply { complete() }
        val emitter = SseUtil.createSseEmitter(
            timeout = Long.MAX_VALUE,
            onCompletion = { unsubscribe(streamerId) },
            onTimeout = { unsubscribe(streamerId) },
            onError = { unsubscribe(streamerId) }
        )
        
        emitters[streamerId] = emitter
        SseUtil.emitEvent(
            SseEvent.STREAMER_SSE_INITIALIZATION,
            emitter,
            mapOf(
                "status" to "OK",
                "message" to "SSE 연결이 성공적으로 설정되었습니다."
            )
        )
        
        log.debug("스트리머 {}의 새로운 SSE 연결을 초기화했습니다.", streamerId)
        return emitter
    }
    
    fun emitStreamerEvent(event: SseEvent, streamerId: Long?, data: Any) {
        emitters[streamerId]?.let { emitter ->
            runAsync({
                try {
                    SseUtil.emitEvent(event, emitter, data)
                } catch (error: Exception) {
                    log.error("스트리머 ID: {}에게 이벤트 전송에 실패했습니다. 오류: {}. 재시도합니다.", streamerId, error.message ?: "알 수 없는 오류")
                    try {
                        SseUtil.emitEvent(event, emitter, data)
                    } catch (retryError: Exception) {
                        log.error("재시도 후에도 스트리머 ID: {}에게 이벤트 전송에 실패했습니다. 오류: {}", streamerId, retryError.message ?: "알 수 없는 오류")
                    }
                }
            }, taskExecutor)
        }
    }
    
    @LogExecutionTime
    fun closeAllSessionEmitters() {
        val futures = emitters.values.map { emitter ->
            runAsync({
                runCatching {
                    emitter.complete()
                }.onSuccess {
                    log.info("SSE 연결이 성공적으로 종료되었습니다.")
                }.onFailure { error ->
                    log.error("SSE 연결 종료 중 오류가 발생했습니다: {}", error.message ?: "알 수 없는 오류")
                }
            }, taskExecutor)
        }
        allOf(*futures.toTypedArray()).join()
        emitters.clear()
    }
    
    fun unsubscribe(streamerId: Long?) =
            emitters.remove(streamerId)?.apply { complete() }
    
}