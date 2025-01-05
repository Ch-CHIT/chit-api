package com.chit.app.global.base

import com.chit.app.global.util.NanoIdUtil
import io.swagger.v3.oas.annotations.Hidden
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api")
class BaseController(
        @Value("\${chzzk.auth.clientId}")
        private val clientId: String,
        @Value("\${chzzk.auth.urls.authorization}")
        private val authorizationUri: String,
        @Value("\${chzzk.auth.urls.redirect}")
        private val redirectUri: String
) {
    @Hidden
    @GetMapping
    fun redirectToLogin(
            response: HttpServletResponse
    ) {
        val redirectUrl = "$authorizationUri?clientId=$clientId&redirectUri=$redirectUri&state=${NanoIdUtil.generate()}"
        response.sendRedirect(redirectUrl)
    }
}