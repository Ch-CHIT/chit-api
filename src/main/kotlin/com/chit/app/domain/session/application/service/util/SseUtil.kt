package com.chit.app.domain.session.application.service.util

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.global.common.logging.logger
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
            safeComplete(emitter, e)
        } catch (e: IllegalStateException) {
            log.error("Emitter가 이미 닫혀 있어 데이터를 보낼 수 없습니다 - 이벤트: {}", event.name)
            safeComplete(emitter, e)
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
            onCompletion { onCompletion() }
            onTimeout { onTimeout() }
            onError { throwable -> onError(throwable) }
        }
    }
    
    private fun safeComplete(emitter: SseEmitter, e: Throwable) {
        try {
            emitter.completeWithError(e)
        } catch (ex: Exception) {
            log.error("Emitter 종료 처리 중 추가 예외 발생: {}", ex.message, ex)
        }
    }
}