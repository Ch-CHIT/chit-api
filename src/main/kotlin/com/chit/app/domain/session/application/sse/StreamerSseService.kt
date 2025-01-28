package com.chit.app.domain.session.application.sse

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.application.sse.SseUtil.createSseEmitter
import com.chit.app.domain.session.application.sse.SseUtil.emitEvent
import com.chit.app.global.delegate.logger
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

@Service
class StreamerSseService(
        private val executor: ExecutorService
) {
    
    private val log = logger<StreamerSseService>()
    private val emitters = ConcurrentHashMap<Long, SseEmitter>()
    
    fun subscribe(streamerId: Long): SseEmitter {
        emitters[streamerId]?.let { unsubscribe(streamerId) }
        
        val emitter = createSseEmitter(
            timeout = Long.MAX_VALUE,
            onCompletion = { unsubscribe(streamerId) },
            onTimeout = { unsubscribe(streamerId) },
            onError = { unsubscribe(streamerId) }
        )
        
        emitters[streamerId] = emitter
        emitEvent(
            emitter, SseEvent.STREAMER_SSE_INITIALIZATION, mapOf(
                "status" to "OK",
                "message" to "SSE 연결이 성공적으로 설정되었습니다."
            )
        )
        
        log.info("스트리머 {}의 새로운 SSE 연결을 초기화했습니다.", streamerId)
        return emitter
    }
    
    fun publishEvent(streamerId: Long?, event: SseEvent, data: Any) =
            emitters[streamerId]
                    ?.let { emitter ->
                        CompletableFuture.runAsync({
                            runCatching {
                                emitEvent(emitter, event, data)
                            }.onSuccess {
                                log.debug("스트리머 ID: {}에게 이벤트 '{}'를 성공적으로 전송했습니다. 데이터: {}", streamerId, event.name, data)
                            }.onFailure { error ->
                                unsubscribe(streamerId).also {
                                    log.error(
                                        "스트리머 ID: {}에게 이벤트 전송에 실패했습니다. 오류: {}",
                                        streamerId,
                                        error.message ?: "알 수 없는 오류"
                                    )
                                }
                            }
                        }, executor)
                    }
    
    
    fun closeAllSessions() {
        val futures = emitters.values.map { emitter ->
            CompletableFuture.runAsync({
                runCatching {
                    emitter.complete()
                }.onSuccess {
                    log.info("SSE 연결이 성공적으로 종료되었습니다.")
                }.onFailure { error ->
                    log.error("SSE 연결 종료 중 오류가 발생했습니다: {}", error.message ?: "알 수 없는 오류")
                }
            }, executor)
        }
        
        CompletableFuture.allOf(*futures.toTypedArray()).join()
        emitters.clear()
    }
    
    fun unsubscribe(streamerId: Long?) =
            emitters.remove(streamerId)?.apply { complete() }
    
}