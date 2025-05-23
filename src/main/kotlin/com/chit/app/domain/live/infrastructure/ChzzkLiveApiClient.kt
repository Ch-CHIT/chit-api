package com.chit.app.domain.live.infrastructure

import com.chit.app.domain.live.domain.exception.InvalidLiveApiRequestException
import com.chit.app.domain.live.domain.exception.LiveFetchException
import com.chit.app.domain.live.domain.exception.LiveNotFoundException
import com.chit.app.domain.live.infrastructure.response.LiveDetailResponse
import com.chit.app.global.common.logging.logger
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
    
    fun fetchChzzkLiveDetail(channelId: String): LiveDetailResponse.Content {
        log.info("[요청] 라이브 상세 정보 요청 (channelId={})", channelId)
        val liveDetailResponse: LiveDetailResponse? = try {
            restClient.get()
                    .uri("$chzzkLiveDetailApiUrl/$channelId/live-detail")
                    .retrieve()
                    .body(LiveDetailResponse::class.java)
        } catch (e: HttpClientErrorException) {
            log.error("잘못된 API 요청 경로: channelId=$channelId", e)
            throw InvalidLiveApiRequestException(cause = e)
        } catch (e: Exception) {
            log.error("라이브 정보 불러오기 실패: channelId=$channelId", e)
            throw LiveFetchException(cause = e)
        }
        if (liveDetailResponse?.content == null) {
            log.warn("[실패] 라이브 상세 정보 없음 (channelId={})", channelId)
            throw LiveNotFoundException()
        }
        
        log.info("[성공] 라이브 상세 정보 응답 수신 완료 (channelId={})", channelId)
        return liveDetailResponse.content
    }
    
}