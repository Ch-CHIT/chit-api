package com.chit.app.domain.session.domain.exception

import com.chit.app.global.common.logging.logger
import com.chit.app.global.response.ErrorResponse
import com.chit.app.global.response.ErrorResponse.Companion.failWithMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice(basePackages = ["com.chit.app.domain.session"])
class SessionExceptionHandler {
    
    private val log = logger<SessionExceptionHandler>()
    
    @ExceptionHandler(DuplicateContentsSessionException::class)
    fun handleDuplicateSession(ex: DuplicateContentsSessionException): ResponseEntity<ErrorResponse> {
        log.error("DuplicateContentsSessionException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(NoOpenContentsSessionException::class)
    fun handleNoOpenSession(ex: NoOpenContentsSessionException): ResponseEntity<ErrorResponse> {
        log.error("NoOpenContentsSessionException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.NOT_FOUND, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(ParticipantNotFoundException::class)
    fun handleParticipantNotFound(ex: ParticipantNotFoundException): ResponseEntity<ErrorResponse> {
        log.error("ParticipantNotFoundException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.NOT_FOUND, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(GameParticipationCodeNotFoundException::class)
    fun handleGameCodeNotFound(ex: GameParticipationCodeNotFoundException): ResponseEntity<ErrorResponse> {
        log.error("GameParticipationCodeNotFoundException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.NOT_FOUND, ex.message, ex.errorCode)
    }
    
}