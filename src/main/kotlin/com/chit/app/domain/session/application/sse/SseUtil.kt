package com.chit.app.domain.session.application.sse

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.global.delegate.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.io.IOException

object SseUtil {
    
    private val log = logger<SseUtil>()
    
    fun emitEvent(
            emitter: SseEmitter,
            event: SseEvent,
            data: Any
    ) {
        try {
            emitter.send(
                SseEmitter.event()
                        .name(event.name)
                        .data(data)
            )
        } catch (e: IOException) {
            log.warn("클라이언트와의 연결이 끊겼습니다 - 이벤트: {}", event.name)
            emitter.completeWithError(e)
        } catch (e: IllegalStateException) {
            log.error("Emitter가 이미 닫혀 있어 데이터를 보낼 수 없습니다 - 이벤트: {}", event.name)
            emitter.completeWithError(e)
        } catch (e: Exception) {
            log.error("알 수 없는 오류 발생 - 이벤트: {}, 에러: {}", event.name, e.message, e)
            throw e
        }
    }
    
    fun createSseEmitter(
            timeout: Long,
            onCompletion: () -> Unit,
            onTimeout: () -> Unit,
            onError: (Throwable) -> Unit
    ): SseEmitter {
        return SseEmitter(timeout).apply {
            onCompletion { onCompletion }
            onTimeout { onTimeout }
            onError { throwable -> onError(throwable) }
        }
    }
    
    suspend fun completeAllEmitters(
            emitters: Collection<SseEmitter>,
            onSuccess: () -> Unit = {},
            onFailure: (Throwable) -> Unit = {}
    ) = coroutineScope {
        emitters.map { emitter ->
            launch(Dispatchers.IO) {
                runCatching {
                    emitter.complete()
                }.onSuccess {
                    onSuccess()
                }.onFailure { error ->
                    onFailure(error)
                }
            }
        }
    }
    
}