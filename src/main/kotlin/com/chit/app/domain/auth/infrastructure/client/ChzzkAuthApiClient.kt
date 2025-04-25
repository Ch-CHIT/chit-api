package com.chit.app.domain.auth.infrastructure.client

import com.chit.app.domain.auth.domain.exception.AuthAccessDeniedException
import com.chit.app.domain.auth.domain.exception.AuthApiPathNotFoundException
import com.chit.app.domain.auth.domain.exception.AuthChannelFetchException
import com.chit.app.domain.auth.domain.exception.AuthCodeForbiddenException
import com.chit.app.domain.auth.domain.exception.AuthTokenRequestException
import com.chit.app.domain.auth.domain.exception.AuthUnauthorizedException
import com.chit.app.domain.auth.domain.exception.InvalidAuthCodeStateException
import com.chit.app.domain.auth.domain.exception.InvalidChannelInfoException
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
    
    private val restClient = RestClient.builder()
            .defaultHeaders { headers -> headers.contentType = MediaType.APPLICATION_JSON }
            .build()
    
    fun fetchChzzkAccessToken(code: String, state: String): String =
            try {
                restClient
                        .post()
                        .uri(tokenUri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(TokenRequest(grantType, clientId, clientSecret, code, state))
                        .retrieve()
                        .body(TokenResponse::class.java)?.content?.accessToken
                        ?: throw InvalidAuthCodeStateException()
            } catch (e: HttpClientErrorException.Forbidden) {
                throw AuthCodeForbiddenException(cause = e)
            } catch (e: Exception) {
                throw AuthTokenRequestException(cause = e)
            }
    
    fun fetchChzzkChannelInfo(accessToken: String): ChannelInfo.Content =
            try {
                restClient
                        .get()
                        .uri(userUri)
                        .header("Authorization", "Bearer $accessToken")
                        .retrieve()
                        .body(ChannelInfo::class.java)?.content
                        ?: throw InvalidChannelInfoException()
            } catch (e: HttpClientErrorException.Unauthorized) {
                throw AuthUnauthorizedException(cause = e)
            } catch (e: HttpClientErrorException.NotFound) {
                throw AuthApiPathNotFoundException(cause = e)
            } catch (e: HttpClientErrorException.Forbidden) {
                throw AuthAccessDeniedException(cause = e)
            } catch (e: Exception) {
                throw AuthChannelFetchException(cause = e)
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