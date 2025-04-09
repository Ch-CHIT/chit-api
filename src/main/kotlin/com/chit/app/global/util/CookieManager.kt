package com.chit.app.global.util

import jakarta.servlet.http.Cookie
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CookieManager(
        @Value("\${jwt.cookie.secure}")
        private val isSecure: Boolean
) {
    
    fun getCookie(request: HttpServletRequest, cookieInfo: CookieInfo): Cookie? =
            request.cookies?.find { it.name == cookieInfo.name }
    
    fun hasCookie(request: HttpServletRequest, cookieInfo: CookieInfo): Boolean =
            getCookie(request, cookieInfo) != null
    
    fun addCookie(
            response: HttpServletResponse,
            cookieInfo: CookieInfo,
            value: String
    ) {
        createCookie(cookieInfo, value).also { response.addCookie(it) }
    }
    
    fun deleteCookie(
            request: HttpServletRequest,
            response: HttpServletResponse,
            cookieInfo: CookieInfo
    ) {
        getCookie(request, cookieInfo)
                ?.let { cookie ->
                    createCookie(cookieInfo, "")
                            .apply { maxAge = 0 }
                            .also { response.addCookie(it) }
                }
    }
    
    private fun createCookie(cookieInfo: CookieInfo, value: String): Cookie =
            Cookie(cookieInfo.name, value)
                    .apply {
                        path = "/"
                        maxAge = cookieInfo.maxAge
                        isHttpOnly = true
                        secure = isSecure
                    }
}