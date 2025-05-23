package com.chit.app.domain.auth.infrastructure.security

import com.chit.app.domain.auth.domain.exception.*
import com.chit.app.global.common.logging.logger
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.time.Instant
import java.util.*

@Component
class TokenProvider {
    
    private val log = logger<TokenProvider>()
    
    @Value("\${jwt.secret-key}")
    private lateinit var secretKey: String
    
    @Value("\${jwt.access-token-validity-in-seconds}")
    private var accessTokenValidityInSeconds: Long = 0L
    
    @Value("\${jwt.refresh-token-validity-in-seconds}")
    private var refreshTokenValidityInSeconds: Long = 0L
    
    private lateinit var key: Key
    
    @PostConstruct
    fun init() {
        key = Keys.hmacShaKeyFor(secretKey.toByteArray())
    }
    
    fun createAccessToken(memberId: Long?, channelId: String?, channelName: String?): String {
        val now = Instant.now()
        val validity = now.plusSeconds(accessTokenValidityInSeconds)
        return Jwts.builder()
                .setSubject(memberId.toString())
                .claim("channelId", channelId)
                .claim("channelName", channelName)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(validity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact()
    }
    
    fun createRefreshToken(memberId: Long?): String {
        val now = Instant.now()
        val validity = now.plusSeconds(refreshTokenValidityInSeconds)
        return Jwts.builder()
                .setSubject(memberId.toString())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(validity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact()
    }
    
    fun getMemberIdFromToken(token: String): Long {
        return getClaims(token).subject.toLong()
    }
    
    fun validateToken(token: String): Boolean {
        return try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
            true
        } catch (e: ExpiredJwtException) {
            log.warn("만료된 JWT 토큰: {}", e.message)
            throw e
        } catch (e: SecurityException) {
            log.warn("잘못된 서명: {}", e.message)
            false
        } catch (e: MalformedJwtException) {
            log.warn("JWT 토큰 형식 오류: {}", e.message)
            false
        } catch (e: UnsupportedJwtException) {
            log.warn("지원되지 않는 JWT 토큰: {}", e.message)
            false
        } catch (e: IllegalArgumentException) {
            log.warn("잘못된 토큰 값: {}", e.message)
            false
        }
    }
    
    private fun getClaims(token: String): Claims {
        return try {
            Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .body
        } catch (e: Exception) {
            when (e) {
                is ExpiredJwtException     -> {
                    log.warn("만료된 토큰: {}", e.message)
                    throw ExpiredTokenException()
                }
                
                is UnsupportedJwtException -> {
                    log.warn("지원되지 않는 토큰: {}", e.message)
                    throw UnsupportedTokenException()
                }
                
                is MalformedJwtException   -> {
                    log.warn("JWT 토큰 형식 오류: {}", e.message)
                    throw MalformedTokenException()
                }
                
                is SecurityException       -> {
                    log.warn("잘못된 서명: {}", e.message)
                    throw InvalidSignatureException()
                }
                
                else                       -> {
                    log.error("예상치 못한 토큰 검증 오류 발생", e)
                    throw InvalidTokenException()
                }
            }
        }
    }
}