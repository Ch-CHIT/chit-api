package com.chit.app.domain.auth.presentation

import com.chit.app.domain.auth.application.AuthService
import com.chit.app.domain.auth.presentation.dto.LoginRequestDto
import com.chit.app.global.common.response.SuccessResponse.Companion.success
import com.chit.app.global.common.response.SuccessResponse.Companion.successWithData
import com.chit.app.global.delegate.EmptyResponse
import com.chit.app.global.delegate.MessageResponse
import com.chit.app.global.util.CookieInfo
import com.chit.app.global.util.CookieManager
import io.jsonwebtoken.JwtException
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
        private val cookieManager: CookieManager,
        private val authService: AuthService,
) {
    
    @PostMapping("/login")
    fun login(
            @RequestBody @Valid request: LoginRequestDto,
            httpResponse: HttpServletResponse
    ): MessageResponse {
        val tokenInfo = authService.login(request.code, request.state)
        cookieManager.addCookie(httpResponse, CookieInfo.REFRESH_TOKEN, tokenInfo.refreshToken)
        return successWithData(tokenInfo.accessToken)
    }
    
    @PostMapping("/logout")
    fun logout(
            request: HttpServletRequest,
            response: HttpServletResponse,
    ): EmptyResponse {
        cookieManager.deleteCookie(request, response, CookieInfo.REFRESH_TOKEN)
        return success()
    }
    
    @PostMapping("/refresh")
    fun refreshAccessToken(
            request: HttpServletRequest,
            response: HttpServletResponse,
    ): MessageResponse {
        if (!cookieManager.hasCookie(request, CookieInfo.REFRESH_TOKEN)) {
            throw JwtException("리프레시 토큰이 존재하지 않습니다.")
        }
        
        val refreshToken = cookieManager.getCookie(request, CookieInfo.REFRESH_TOKEN)?.value
                ?: throw JwtException("리프레시 토큰을 가져올 수 없습니다.")
        
        val tokenInfo = authService.refreshAccessToken(refreshToken)
        cookieManager.addCookie(response, CookieInfo.REFRESH_TOKEN, tokenInfo.refreshToken)
        
        return successWithData(tokenInfo.accessToken)
    }
    
}