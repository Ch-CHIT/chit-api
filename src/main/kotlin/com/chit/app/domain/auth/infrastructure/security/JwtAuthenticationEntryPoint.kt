package com.chit.app.domain.auth.infrastructure.security

import com.chit.app.global.delegate.logger
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

@Component
class JwtAuthenticationEntryPoint(
        private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {
    
    private val log = logger<JwtAuthenticationEntryPoint>()
    
    override fun commence(
            request: HttpServletRequest,
            response: HttpServletResponse,
            authException: AuthenticationException
    ) {
        
        log.error("인증되지 않은 사용자의 접근 - URI: {}, 메시지: {}", request.requestURI, authException.message)
        
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = "UTF-8"
        
        val errorResponse = mapOf(
            "status" to HttpStatus.UNAUTHORIZED.value(),
            "error" to "Unauthorized",
            "message" to "인증되지 않은 사용자입니다.",
            "path" to request.requestURI
        )
        
        response.writer.write(objectMapper.writeValueAsString(errorResponse))
    }
}