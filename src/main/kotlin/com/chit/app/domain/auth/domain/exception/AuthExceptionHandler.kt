package com.chit.app.domain.auth.domain.exception

import com.chit.app.global.common.logging.logger
import com.chit.app.global.response.ErrorResponse
import com.chit.app.global.response.ErrorResponse.Companion.failWithMessage
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice


@RestControllerAdvice
class AuthExceptionHandler {
    
    private val log = logger<AuthExceptionHandler>()
    
    @ExceptionHandler(TokenReissueException::class)
    fun handleTokenReissue(ex: TokenReissueException): ResponseEntity<ErrorResponse> {
        log.error("TokenReissueException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(MissingTokenException::class)
    fun handleMissingTokenException(ex: MissingTokenException): ResponseEntity<ErrorResponse> {
        log.error("MissingTokenException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(InvalidAuthCodeStateException::class)
    fun handleInvalidAuthCodeState(ex: InvalidAuthCodeStateException): ResponseEntity<ErrorResponse> {
        log.error("InvalidAuthCodeStateException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(AuthTokenRequestException::class)
    fun handleAuthTokenRequest(ex: AuthTokenRequestException): ResponseEntity<ErrorResponse> {
        log.error("AuthTokenRequestException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(AuthCodeForbiddenException::class)
    fun handleAuthCodeForbidden(ex: AuthCodeForbiddenException): ResponseEntity<ErrorResponse> {
        log.error("AuthCodeForbiddenException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.FORBIDDEN, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(InvalidChannelInfoException::class)
    fun handleInvalidChannelInfo(ex: InvalidChannelInfoException): ResponseEntity<ErrorResponse> {
        log.error("InvalidChannelInfoException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidTokenException(ex: InvalidTokenException): ResponseEntity<ErrorResponse> {
        log.error("InvalidTokenException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.UNAUTHORIZED, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(ExpiredTokenException::class)
    fun handleExpiredToken(ex: ExpiredTokenException): ResponseEntity<ErrorResponse> {
        log.error("ExpiredTokenException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.UNAUTHORIZED, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(UnsupportedTokenException::class)
    fun handleUnsupportedToken(ex: UnsupportedTokenException): ResponseEntity<ErrorResponse> {
        log.error("UnsupportedTokenException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(MalformedTokenException::class)
    fun handleMalformedToken(ex: MalformedTokenException): ResponseEntity<ErrorResponse> {
        log.error("MalformedTokenException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(InvalidSignatureException::class)
    fun handleInvalidSignature(ex: InvalidSignatureException): ResponseEntity<ErrorResponse> {
        log.error("InvalidSignatureException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.BAD_REQUEST, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(AuthUnauthorizedException::class)
    fun handleAuthUnauthorized(ex: AuthUnauthorizedException): ResponseEntity<ErrorResponse> {
        log.error("AuthUnauthorizedException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.UNAUTHORIZED, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(AuthApiPathNotFoundException::class)
    fun handleAuthApiPathNotFound(ex: AuthApiPathNotFoundException): ResponseEntity<ErrorResponse> {
        log.error("AuthApiPathNotFoundException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.NOT_FOUND, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(AuthAccessDeniedException::class)
    fun handleAuthAccessDenied(ex: AuthAccessDeniedException): ResponseEntity<ErrorResponse> {
        log.error("AuthAccessDeniedException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.FORBIDDEN, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(AuthChannelFetchException::class)
    fun handleAuthChannelFetch(ex: AuthChannelFetchException): ResponseEntity<ErrorResponse> {
        log.error("AuthChannelFetchException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, ex.message, ex.errorCode)
    }
    
    @ExceptionHandler(AuthenticatedUserNotFoundException::class)
    fun handleAuthenticatedUserNotFound(ex: AuthenticatedUserNotFoundException): ResponseEntity<ErrorResponse> {
        log.error("AuthenticatedUserNotFoundException: {}", ex.message, ex)
        return failWithMessage(HttpStatus.UNAUTHORIZED, ex.message, ex.errorCode)
    }
    
}