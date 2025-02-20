package com.chit.app.domain.live.infrastructure

import com.chit.app.domain.live.infrastructure.response.LiveDetailResponse
import com.chit.app.global.delegate.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient

@Component
class ChzzkLiveApiClient(
        
        @Value("\${chzzk.live.info}")
        private val chzzkLiveDetailApiUrl: String

) {
    
    private val log = logger<ChzzkLiveApiClient>()
    private val restClient = RestClient.builder()
            .defaultHeaders { headers -> headers.contentType = MediaType.APPLICATION_JSON }
            .build()
    
    fun fetchChzzkLiveDetail(channelId: String): LiveDetailResponse.Content? {
        return try {
            restClient.get()
                    .uri("$chzzkLiveDetailApiUrl/$channelId/live-detail")
                    .retrieve()
                    .body(LiveDetailResponse::class.java)?.content
        } catch (e: HttpClientErrorException) {
            log.error("잘못된 API 요청 경로: channelId=$channelId", e)
            throw IllegalArgumentException("잘못된 API 요청 경로입니다. 관리자에게 문의해 주세요.")
        } catch (e: Exception) {
            log.error("라이브 정보 불러오기 실패: channelId=$channelId", e)
            throw IllegalStateException("라이브 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.")
        }
    }
    
}