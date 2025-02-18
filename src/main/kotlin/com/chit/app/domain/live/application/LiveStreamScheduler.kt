package com.chit.app.domain.live.application

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.live.infrastructure.ChzzkLiveApiClient
import com.chit.app.global.delegate.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime.now
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.ExecutorService
import kotlin.system.measureTimeMillis

@Component
@Transactional
class LiveStreamScheduler(
        private val taskExecutor: ExecutorService,
        private val chzzkLiveApiClient: ChzzkLiveApiClient,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    private val log = logger<LiveStreamScheduler>()
    
    @Scheduled(cron = "0 0 * * * *")
    fun refreshLiveStreamStatus() {
        val currentOpenLiveStreams = liveStreamRepository.findAllOpenLiveStreams()
        log.debug("refreshLiveStreamStatus() 호출 - 업데이트할 라이브 스트림 수: ${currentOpenLiveStreams.size}")
        val elapsedTime = measureTimeMillis {
            val futures = currentOpenLiveStreams.map { openLiveStream ->
                supplyAsync({
                    try {
                        val liveDetail = chzzkLiveApiClient.fetchChzzkLiveDetail(openLiveStream.channelId!!)
                        liveDetail?.let {
                            if (openLiveStream.liveId != it.liveId) {
                                // 라이브 ID가 다르면 스트림 종료 처리
                                openLiveStream.apply {
                                    liveStatus = LiveStatus.CLOSE
                                    closedDate = now()
                                }
                            } else {
                                openLiveStream.update(
                                    liveTitle = it.liveTitle,
                                    liveStatus = it.status,
                                    categoryType = it.categoryType,
                                    liveCategory = it.liveCategory,
                                    liveCategoryValue = it.liveCategoryValue,
                                    openDate = it.openDate,
                                    closeDate = it.closeDate
                                )
                                log.debug(
                                    "라이브 스트림 정보를 업데이트했습니다 - 채널 ID: {}, 상태: {}, 제목: {}, 카테고리: {}, 시작 시간: {}, 종료 시간: {}",
                                    openLiveStream.channelId,
                                    it.status,
                                    it.liveTitle,
                                    it.categoryType,
                                    it.openDate,
                                    it.closeDate
                                )
                            }
                            openLiveStream
                        }
                    } catch (e: Exception) {
                        log.error("라이브 스트림 업데이트 중 오류 발생 - 채널 ID: ${openLiveStream.channelId}, 오류: ${e.message}", e)
                        null
                    }
                }, taskExecutor)
            }
            
            val results = futures.mapNotNull { it.join() }
            liveStreamRepository.batchUpdateLiveStreams(results)
            log.debug("총 ${results.size}개의 라이브 스트림이 업데이트되었습니다.")
        }
        log.debug("전체 작업 소요 시간: ${elapsedTime}ms")
    }
    
}