package com.chit.app.domain.live.infrastructure

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.global.delegate.logger
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.RestClient
import java.time.LocalDateTime

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
        return runCatching {
            restClient.get()
                    .uri("$chzzkLiveDetailApiUrl/$channelId/live-detail")
                    .retrieve()
                    .body(LiveDetailResponse::class.java)?.content
        }.getOrElse { e ->
            when (e) {
                is HttpClientErrorException -> throw IllegalArgumentException("잘못된 API 요청 경로입니다. 관리자에게 문의해 주세요.", e)
                else                        -> throw IllegalStateException("라이브 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.", e)
            }
        }.also { log.info("채널 ID={}에 대한 라이브 상세 정보를 가져왔습니다.", channelId) }
    }
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class LiveDetailResponse(
            val code: Int?,
            val message: String?,
            val content: Content?
    ) {
        data class Content(
                val liveId: Long,
                val liveTitle: String,
                val status: LiveStatus,
                val categoryType: String,
                val liveCategory: String,
                val liveCategoryValue: String,
                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                val openDate: LocalDateTime,
                @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
                val closeDate: LocalDateTime?,
        )
    }
    
}