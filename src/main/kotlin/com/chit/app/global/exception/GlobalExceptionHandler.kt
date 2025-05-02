package com.chit.app.global.exception

import com.chit.app.global.common.logging.logger
import com.chit.app.global.common.response.ErrorResponse
import com.chit.app.global.common.response.ErrorResponse.Companion.failWithErrors
import com.chit.app.global.common.response.ErrorResponse.Companion.failWithMessage
import com.chit.app.global.common.response.ErrorResponse.Companion.internalErrorWithMessage
import jakarta.validation.ConstraintViolationException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

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
        return failWithErrors(HttpStatus.BAD_REQUEST, errors, 500001)
    }
    
    @ExceptionHandler(ConstraintViolationException::class)
    fun handle(ex: ConstraintViolationException): ResponseEntity<ErrorResponse> {
        log.error("ConstraintViolationException: {}", ex.message, ex)
        val errors = ex.constraintViolations.associate { it.propertyPath.toString() to it.message }
        return failWithErrors(HttpStatus.BAD_REQUEST, errors, 500002)
    }
    
    @ExceptionHandler(DataIntegrityException::class)
    fun handleDataIntegrityPersistence(ex: DataIntegrityException): ResponseEntity<ErrorResponse> {
        log.error("DataIntegrityException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(OptimisticLockingConflictException::class)
    fun handleOptimisticLockingPersistence(ex: OptimisticLockingConflictException): ResponseEntity<ErrorResponse> {
        log.error("OptimisticLockingConflictException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.CONFLICT, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(TransactionSystemFailureException::class)
    fun handleTransactionFailurePersistence(ex: TransactionSystemFailureException): ResponseEntity<ErrorResponse> {
        log.error("TransactionSystemFailureException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(GlobalPersistenceException::class)
    fun handleUnknownPersistenceError(ex: GlobalPersistenceException): ResponseEntity<ErrorResponse> {
        log.error("GlobalPersistenceException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, ex.message, ex.errorCode)
    }
    
}