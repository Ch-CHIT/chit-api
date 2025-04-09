package com.chit.app.domain.auth.presentation.filter

import com.chit.app.domain.auth.infrastructure.properties.JwtFilterProperties
import com.chit.app.domain.auth.infrastructure.security.TokenProvider
import com.chit.app.global.common.logging.logger
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.UnsupportedJwtException
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.util.AntPathMatcher
import org.springframework.web.filter.OncePerRequestFilter

class JwtAuthenticationFilter(
        private val tokenProvider: TokenProvider,
        private val properties: JwtFilterProperties
) : OncePerRequestFilter() {
    
    private val log = logger<JwtAuthenticationFilter>()
    
    companion object {
        private const val AUTHORIZATION_HEADER = "Authorization"
        private const val BEARER_PREFIX = "Bearer "
        private val PATH_MATCHER = AntPathMatcher()
    }
    
    override fun doFilterInternal(
            request: HttpServletRequest,
            response: HttpServletResponse,
            filterChain: FilterChain
    ) {
        try {
            when {
                request.isWhitelistedUrl() -> {
                    filterChain.doFilter(request, response)
                    return
                }
                
                request.isSseUrl()         -> {
                    handleAuthentication(request, isSse = true)
                }
                
                else                       -> {
                    handleAuthentication(request, isSse = false)
                }
            }
        } catch (e: ExpiredJwtException) {
            handleExpiredJwtException(e, response)
            return
        } catch (e: MalformedJwtException) {
            log.error("잘못된 형식의 JWT 토큰입니다: ${e.message}", e)
        } catch (e: UnsupportedJwtException) {
            log.error("지원되지 않는 JWT 토큰입니다: ${e.message}", e)
        } catch (e: SecurityException) {
            log.error("잘못된 JWT 서명입니다: ${e.message}", e)
        } catch (e: IllegalArgumentException) {
            log.error("JWT 토큰이 잘못되었습니다: ${e.message}", e)
        } catch (e: Exception) {
            log.error("인증 필터에서 알 수 없는 예외 발생: ${e.message}", e)
        }
        
        filterChain.doFilter(request, response)
    }
    
    private fun handleAuthentication(request: HttpServletRequest, isSse: Boolean) {
        val token = if (isSse) request.extractSseToken() else request.extractBearerToken()
        token?.takeIf { tokenProvider.validateToken(it) }
                ?.let { validToken ->
                    SecurityContextHolder.getContext().authentication = UsernamePasswordAuthenticationToken(
                        tokenProvider.getMemberIdFromToken(validToken),
                        null,
                        listOf(SimpleGrantedAuthority("ROLE_USER"))
                    )
                }
    }
    
    private fun HttpServletRequest.isWhitelistedUrl(): Boolean =
            properties.whitelistUrls.any { pattern -> pattern.matchUrl(requestURI) }
    
    private fun HttpServletRequest.isSseUrl(): Boolean =
            properties.sseUrlPatterns.any { pattern -> pattern.matchUrl(requestURI) }
    
    private fun String.matchUrl(uri: String): Boolean = PATH_MATCHER.match(this, uri)
    
    private fun HttpServletRequest.extractBearerToken(): String? =
            getHeader(AUTHORIZATION_HEADER)
                    ?.takeIf { it.isNotBlank() && it.startsWith(BEARER_PREFIX) }
                    ?.substring(BEARER_PREFIX.length)
    
    private fun HttpServletRequest.extractSseToken(): String? =
            getParameter("accessToken")?.takeIf { it.isNotBlank() }
    
    private fun handleExpiredJwtException(e: ExpiredJwtException, response: HttpServletResponse) {
        log.warn("만료된 JWT 토큰입니다: ${e.message}")
        response.status = HttpServletResponse.SC_UNAUTHORIZED
        response.contentType = "application/json"
        response.characterEncoding = "UTF-8"
        response.writer.write(
            """
        {
          "status": 401,
          "data": "EXPIRED_TOKEN",
          "message": "액세스 토큰이 만료되었습니다."
        }
        """.trimIndent()
        )
        response.writer.flush()
    }
}