package com.chit.app.global.handler

import com.chit.app.global.delegate.logger
import com.chit.app.global.response.ErrorResponse
import com.chit.app.global.response.ErrorResponse.Companion.failWithErrors
import com.chit.app.global.response.ErrorResponse.Companion.failWithMessage
import com.chit.app.global.response.ErrorResponse.Companion.internalErrorWithMessage
import io.jsonwebtoken.JwtException
import jakarta.validation.ConstraintViolationException
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationServiceException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.client.HttpClientErrorException
import javax.naming.AuthenticationException

@RestControllerAdvice
class GlobalExceptionHandler {
    
    private val log = logger<GlobalExceptionHandler>()
    
    @ExceptionHandler(RuntimeException::class)
    fun handle(ex: RuntimeException): ResponseEntity<ErrorResponse> {
        log.error("RuntimeException: {}", ex.message, ex)
        return internalErrorWithMessage(ex.message ?: "Internal Server Error")
    }
    
    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(ex: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        log.error("MethodArgumentNotValidException: {}", ex.message, ex)
        val errors = ex.bindingResult.fieldErrors.associate {
            it.field to (it.defaultMessage ?: "유효하지 않은 값입니다.")
        }
        return failWithErrors(HttpStatus.BAD_REQUEST, errors)
    }
    
    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        log.error("ConstraintViolationException: {}", ex.message, ex)
        val errors = ex.constraintViolations.associate { it.propertyPath.toString() to it.message }
        return failWithErrors(HttpStatus.BAD_REQUEST, errors)
    }
    
    @ExceptionHandler(IllegalStateException::class)
    fun handle(ex: IllegalStateException): ResponseEntity<ErrorResponse> {
        log.error("IllegalStateException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message ?: "Bad Request")
    }
    
    @ExceptionHandler(IllegalArgumentException::class)
    fun handle(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.error("IllegalArgumentException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message ?: "Bad Request")
    }
    
    @ExceptionHandler(JwtException::class)
    fun handle(ex: JwtException): ResponseEntity<ErrorResponse> {
        log.error("JwtException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.UNAUTHORIZED, ex.message ?: "Unauthorized")
    }
    
    @ExceptionHandler(AuthenticationException::class)
    fun handle(ex: AuthenticationException): ResponseEntity<ErrorResponse> {
        log.error("AuthenticationException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.UNAUTHORIZED, ex.message ?: "Unauthorized")
    }
    
    @ExceptionHandler(AuthenticationServiceException::class)
    fun handle(ex: AuthenticationServiceException): ResponseEntity<ErrorResponse> {
        log.error("AuthenticationServiceException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.UNAUTHORIZED, ex.message ?: "Unauthorized")
    }
    
    @ExceptionHandler(HttpClientErrorException::class)
    fun handle(ex: HttpClientErrorException): ResponseEntity<ErrorResponse> {
        log.error("HttpClientErrorException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.NOT_FOUND, ex.message ?: "Forbidden")
    }
    
    @ExceptionHandler(InvalidDataAccessApiUsageException::class)
    fun handle(ex: InvalidDataAccessApiUsageException): ResponseEntity<ErrorResponse> {
        log.error("InvalidDataAccessApiUsageException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message ?: "Forbidden")
    }
    
}
