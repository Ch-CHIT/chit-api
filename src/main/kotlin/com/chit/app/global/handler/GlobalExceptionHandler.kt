package com.chit.app.global.handler

import com.chit.app.global.delegate.logger
import com.chit.app.global.response.ErrorResponse
import com.chit.app.global.response.ErrorResponse.Companion.failWithErrors
import com.chit.app.global.response.ErrorResponse.Companion.failWithMessage
import com.chit.app.global.response.ErrorResponse.Companion.internalErrorWithMessage
import io.jsonwebtoken.JwtException
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import javax.naming.AuthenticationException

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val log = logger<GlobalExceptionHandler>()
    
    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        log.error("RuntimeException: {}", ex.message, ex)
        return internalErrorWithMessage(ex.message ?: "Internal Server Error")
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        log.error("MethodArgumentNotValidException: {}", ex.message, ex)
        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "유효하지 않은 값입니다.")
        }
        return failWithErrors(HttpStatus.BAD_REQUEST, errors)
    }
    
    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationExceptions(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        log.error("ConstraintViolationException: {}", ex.message, ex)
        val errors = ex.constraintViolations.associate { it.propertyPath.toString() to it.message }
        return failWithErrors(HttpStatus.BAD_REQUEST, errors)
    }
    
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalStateException(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        log.error("IllegalStateException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message ?: "Bad Request")
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.error("IllegalArgumentException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message ?: "Bad Request")
    }
    
    @ExceptionHandler(JwtException::class)
    fun handleJwtException(ex: JwtException): ResponseEntity<ErrorResponse> {
        log.error("JwtException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.UNAUTHORIZED, ex.message ?: "Unauthorized")
    }
    
    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ErrorResponse> {
        log.error("AuthenticationException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.UNAUTHORIZED, ex.message ?: "Unauthorized")
    }
    
}