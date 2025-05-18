package com.chit.app.domain.auth.infrastructure.client

import com.chit.app.domain.auth.domain.exception.*
import com.chit.app.global.common.logging.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

@Component
class ChzzkAuthApiClient(
        
        @Value("\${chzzk.auth.urls.token}")
        private val tokenUri: String,
        @Value("\${chzzk.auth.urls.user}")
        private val userUri: String,
        @Value("\${chzzk.auth.grantType}")
        private val grantType: String,
        @Value("\${chzzk.auth.clientId}")
        private val clientId: String,
        @Value("\${chzzk.auth.clientSecret}")
        private val clientSecret: String

) {
    private val log = logger<ChzzkAuthApiClient>()
    
    private val restClient = RestClient.builder()
            .defaultHeaders { headers -> headers.contentType = MediaType.APPLICATION_JSON }
            .build()
    
    fun fetchChzzkAccessToken(code: String, state: String): String {
        log.info("[요청] Chzzk 액세스 토큰 요청 (code={}, state={})", code, state)
        try {
            val accessToken = restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(TokenRequest(grantType, clientId, clientSecret, code, state))
                    .retrieve()
                    .body(TokenResponse::class.java)?.content?.accessToken
                    ?: throw InvalidAuthCodeStateException()
            
            log.info("[성공] Chzzk 액세스 토큰 응답 수신 완료")
            return accessToken
        } catch (e: HttpClientErrorException.Forbidden) {
            log.warn("[실패] Chzzk 토큰 요청 거부 - Forbidden (code={}, state={}) [부가정보: ${e.message}]", code, state)
            throw AuthCodeForbiddenException(cause = e)
        } catch (e: Exception) {
            log.error("[실패] Chzzk 액세스 토큰 요청 실패 (code={}, state={}) [부가정보: ${e.message}]", code, state, e)
            throw AuthTokenRequestException(cause = e)
        }
    }
    
    fun fetchChzzkChannelInfo(accessToken: String): ChannelInfo.Content {
        log.info("[요청] Chzzk 채널 정보 요청 (accessToken=****)")
        try {
            val content = restClient
                    .get()
                    .uri(userUri)
                    .header("Authorization", "Bearer $accessToken")
                    .retrieve()
                    .body(ChannelInfo::class.java)?.content
            
            if (content == null) {
                log.warn("[실패] Chzzk 채널 정보 없음 [부가정보: content=null]")
                throw InvalidChannelInfoException()
            }
            
            log.info(
                "[성공] Chzzk 채널 정보 응답 수신 완료 (channelId={}, channelName={}, nickname={})",
                content.channelId,
                content.channelName,
                content.nickname
            )
            return content
        } catch (e: HttpClientErrorException.Unauthorized) {
            log.warn("[실패] Chzzk 채널 정보 요청 실패 - 인증되지 않음 [부가정보: ${e.message}]")
            throw AuthUnauthorizedException(cause = e)
        } catch (e: HttpClientErrorException.NotFound) {
            log.warn("[실패] Chzzk 채널 정보 요청 실패 - API 경로 없음 [부가정보: ${e.message}]")
            throw AuthApiPathNotFoundException(cause = e)
        } catch (e: HttpClientErrorException.Forbidden) {
            log.warn("[실패] Chzzk 채널 정보 요청 실패 - 접근 거부 [부가정보: ${e.message}]")
            throw AuthAccessDeniedException(cause = e)
        } catch (e: Exception) {
            log.error("[실패] Chzzk 채널 정보 요청 실패 [부가정보: ${e.message}]", e)
            throw AuthChannelFetchException(cause = e)
        }
    }
}

data class TokenRequest(
        val grantType: String,
        val clientId: String,
        val clientSecret: String,
        val code: String,
        val state: String
)

data class TokenResponse(
        val code: Int?,
        val message: String?,
        val content: Content?
) {
    data class Content(
            val refreshToken: String?,
            val accessToken: String?,
            val tokenType: String?,
            val expiresIn: Int?,
            val scope: String?
    )
}

data class ChannelInfo(
        val code: Int?,
        val message: String?,
        val content: Content?
) {
    data class Content(
            val channelId: String,
            val channelName: String,
            val nickname: String
    )
}