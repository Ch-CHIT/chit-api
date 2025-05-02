package com.chit.app.domain.live.domain.exception

import com.chit.app.global.common.logging.logger
import com.chit.app.global.common.response.ErrorResponse
import com.chit.app.global.common.response.ErrorResponse.Companion.failWithMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class LiveExceptionHandler {
    
    private val log = logger<LiveExceptionHandler>()
    
    @ExceptionHandler(InvalidLiveApiRequestException::class)
    fun handleInvalidRequest(ex: InvalidLiveApiRequestException): ResponseEntity<ErrorResponse> {
        log.error("InvalidLiveApiRequestException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(LiveFetchException::class)
    fun handleFetchError(ex: LiveFetchException): ResponseEntity<ErrorResponse> {
        log.error("LiveFetchException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(LiveNotFoundException::class)
    fun handleNotFound(ex: LiveNotFoundException): ResponseEntity<ErrorResponse> {
        log.error("LiveNotFoundException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.NOT_FOUND, ex.message, ex.errorCode)
    }
    
}