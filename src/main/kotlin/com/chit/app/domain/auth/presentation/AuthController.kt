package com.chit.app.domain.auth.presentation

import com.chit.app.domain.auth.application.AuthService
import com.chit.app.domain.auth.presentation.annotation.CurrentMemberId
import com.chit.app.domain.auth.presentation.dto.LoginRequestDto
import com.chit.app.domain.live.application.LiveStreamService
import com.chit.app.global.delegate.MessageResponse
import com.chit.app.global.delegate.EmptyResponse
import com.chit.app.global.common.response.SuccessResponse.Companion.success
import com.chit.app.global.common.response.SuccessResponse.Companion.successWithData
import com.chit.app.global.util.CookieInfo
import com.chit.app.global.util.CookieManager
import io.swagger.v3.oas.annotations.Parameter
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
        private val liveStreamService: LiveStreamService
) {
    
    @PostMapping("/login")
    fun login(
            @RequestBody @Valid request: LoginRequestDto,
            httpResponse: HttpServletResponse
    ): MessageResponse {
        val tokenInfo = authService.login(request.code, request.state).also {
            cookieManager.addCookie(httpResponse, CookieInfo.REFRESH_TOKEN, it.refreshToken)
            liveStreamService.saveOrUpdateLiveStream(it.memberId, it.channelId)
        }
        return successWithData(tokenInfo.accessToken)
    }
    
    @PostMapping("/logout")
    fun logout(
            request: HttpServletRequest,
            response: HttpServletResponse,
            @Parameter(hidden = true) @CurrentMemberId currentMemberId: Long
    ): EmptyResponse {
        cookieManager.deleteCookie(request, response, CookieInfo.REFRESH_TOKEN)
        authService.logout(currentMemberId)
        return success()
    }
    
}