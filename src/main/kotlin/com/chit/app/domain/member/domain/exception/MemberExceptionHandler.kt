package com.chit.app.domain.member.domain.exception

import com.chit.app.global.common.logging.logger
import com.chit.app.global.common.response.ErrorResponse
import com.chit.app.global.common.response.ErrorResponse.Companion.failWithMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class MemberExceptionHandler {
    
    private val log = logger<MemberExceptionHandler>()
    
    @ExceptionHandler(MemberRegistrationException::class)
    fun handleRegistration(ex: MemberRegistrationException): ResponseEntity<ErrorResponse> {
        log.error("MemberRegistrationException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(MemberNotFoundException::class)
    fun handleNotFound(ex: MemberNotFoundException): ResponseEntity<ErrorResponse> {
        log.error("MemberNotFoundException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.NOT_FOUND, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(MemberValidationException::class)
    fun handleValidation(ex: MemberValidationException): ResponseEntity<ErrorResponse> {
        log.error("MemberValidationException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
}