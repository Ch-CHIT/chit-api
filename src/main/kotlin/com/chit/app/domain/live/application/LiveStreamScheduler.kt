package com.chit.app.domain.live.application

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.live.infrastructure.ChzzkLiveApiClient
import com.chit.app.global.delegate.logger
import kotlinx.coroutines.*
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import kotlin.system.measureTimeMillis

@Component
@Transactional
class LiveStreamScheduler(
        private val chzzkLiveApiClient: ChzzkLiveApiClient,
        private val liveStreamRepository: LiveStreamRepository,
) {
    private val log = logger<LiveStreamScheduler>()
    
    @Scheduled(cron = "0 0 * * * *")
    fun update() = runBlocking {
        val currentOpenLiveStreams = liveStreamRepository.findAllOpenLiveStreams()
        refreshLiveStreamStatus(currentOpenLiveStreams)
    }
    
    suspend fun refreshLiveStreamStatus(currentOpenLiveStreams: List<LiveStream>) = coroutineScope {
        val elapsedTime = measureTimeMillis {
            val updatedLiveStreams = currentOpenLiveStreams.map { openLiveStream ->
                async(Dispatchers.IO) {
                    try {
                        chzzkLiveApiClient.fetchChzzkLiveDetail(openLiveStream.channelId!!)?.let { liveDetail ->
                            if (openLiveStream.liveId != liveDetail.liveId) {
                                openLiveStream.apply {
                                    liveStatus = LiveStatus.CLOSE
                                    closedDate = LocalDateTime.now()
                                }
                            } else {
                                openLiveStream.update(
                                    liveTitle = liveDetail.liveTitle,
                                    liveStatus = liveDetail.status,
                                    categoryType = liveDetail.categoryType,
                                    liveCategory = liveDetail.liveCategory,
                                    liveCategoryValue = liveDetail.liveCategoryValue,
                                    openDate = liveDetail.openDate,
                                    closeDate = liveDetail.closeDate
                                )
                                log.info(
                                    "라이브 스트림 정보를 업데이트했습니다 - " +
                                            "채널 ID: ${openLiveStream.channelId}, " +
                                            "상태: ${liveDetail.status}, " +
                                            "제목: ${liveDetail.liveTitle}, " +
                                            "카테고리: ${liveDetail.categoryType}, " +
                                            "시작 시간: ${liveDetail.openDate}, " +
                                            "종료 시간: ${liveDetail.closeDate}"
                                )
                            }
                            openLiveStream
                        }
                    } catch (e: Exception) {
                        log.error("라이브 스트림 업데이트 중 오류 발생 - 채널 ID: ${openLiveStream.channelId}, 오류: ${e.message}", e)
                        null
                    }
                }
            }
            
            val results = updatedLiveStreams.awaitAll().filterNotNull()
            liveStreamRepository.batchUpdateLiveStreams(results)
            log.info("총 ${results.size}개의 라이브 스트림이 업데이트되었습니다.")
        }
        
        log.info("전체 작업 소요 시간: ${elapsedTime}ms")
    }
}