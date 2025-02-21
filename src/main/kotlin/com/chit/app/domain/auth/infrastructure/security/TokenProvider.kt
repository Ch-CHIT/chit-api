package com.chit.app.domain.auth.infrastructure.security

import com.chit.app.global.common.logging.logger
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.MalformedJwtException
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.Key
import java.time.Instant
import java.util.Date

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
        } catch (e: Exception) {
            when (e) {
                is SecurityException        -> log.warn("잘못된 서명: {}", e.message)
                is MalformedJwtException    -> log.warn("JWT 토큰 형식 오류: {}", e.message)
                is ExpiredJwtException      -> log.warn("만료된 JWT 토큰: {}", e.message)
                is UnsupportedJwtException  -> log.warn("지원되지 않는 JWT 토큰: {}", e.message)
                is IllegalArgumentException -> log.warn("잘못된 토큰 값: {}", e.message)
                else                        -> {
                    log.error("예상치 못한 토큰 검증 오류 발생", e)
                    throw e
                }
            }
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
                is ExpiredJwtException      -> {
                    log.warn("만료된 토큰: {}", e.message)
                    throw JwtException("만료된 토큰입니다.", e)
                }
                
                is UnsupportedJwtException  -> {
                    log.warn("지원되지 않는 토큰: {}", e.message)
                    throw JwtException("지원되지 않는 토큰입니다.", e)
                }
                
                is MalformedJwtException    -> {
                    log.warn("JWT 토큰 형식 오류: {}", e.message)
                    throw JwtException("형식이 올바르지 않은 토큰입니다.", e)
                }
                
                is SecurityException        -> {
                    log.warn("잘못된 서명: {}", e.message)
                    throw JwtException("잘못된 서명입니다.", e)
                }
                
                is IllegalArgumentException -> {
                    log.warn("잘못된 토큰 값: {}", e.message)
                    throw JwtException("잘못된 토큰입니다.", e)
                }
                
                else                        -> {
                    log.error("예상치 못한 토큰 검증 오류 발생", e)
                    throw e
                }
            }
        }
    }
}