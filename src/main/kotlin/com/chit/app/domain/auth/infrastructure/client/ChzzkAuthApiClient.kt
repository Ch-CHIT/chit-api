package com.chit.app.domain.auth.infrastructure.client

import com.chit.app.global.delegate.logger
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
        return runCatching {
            restClient
                    .post()
                    .uri(tokenUri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(
                        TokenRequest(
                            grantType = grantType,
                            clientId = clientId,
                            clientSecret = clientSecret,
                            code = code,
                            state = state
                        )
                    )
                    .retrieve()
                    .body(TokenResponse::class.java)?.content?.accessToken
                    ?: throw IllegalArgumentException("인증 코드 또는 상태 값이 올바르지 않습니다. 다시 시도해 주세요.")
        }.getOrElse { e ->
            when (e) {
                is HttpClientErrorException.Forbidden -> throw IllegalArgumentException("잘못된 인증 코드입니다. 다시 시도해 주세요.", e)
                else                                  -> throw IllegalStateException("액세스 토큰 발급 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.", e)
            }
        }
    }
    
    fun fetchChzzkChannelInfo(accessToken: String): ChannelInfo.Content {
        return runCatching {
            restClient
                    .get()
                    .uri(userUri)
                    .header("Authorization", "Bearer $accessToken")
                    .retrieve()
                    .body(ChannelInfo::class.java)?.content
                    ?: throw IllegalArgumentException("채널 정보를 가져오는 데 실패했습니다. 유효하지 않은 액세스 토큰이거나 요청 경로가 잘못되었습니다.")
        }.getOrElse { e ->
            when (e) {
                is HttpClientErrorException.Unauthorized -> throw IllegalArgumentException("잘못된 인증 정보입니다. 다시 로그인해 주세요.", e)
                is HttpClientErrorException.NotFound     -> throw IllegalArgumentException("API 경로를 확인해 주세요.", e)
                is HttpClientErrorException.Forbidden    -> throw IllegalStateException("접근 권한이 없습니다. 요청 권한을 확인해 주세요.", e)
                else                                     -> throw IllegalStateException("채널 정보 조회 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.", e)
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
    
}