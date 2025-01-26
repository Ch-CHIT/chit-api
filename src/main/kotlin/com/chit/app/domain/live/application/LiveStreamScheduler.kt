package com.chit.app.domain.live.application

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.live.infrastructure.ChzzkLiveApiClient
import com.chit.app.global.delegate.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

@Component
@Transactional
class LiveStreamScheduler(
        private val executor: ExecutorService,
        private val chzzkLiveApiClient: ChzzkLiveApiClient,
        private val liveStreamRepository: LiveStreamRepository
) {
    private val log = logger<LiveStreamScheduler>()
    
    @Scheduled(cron = "0 0 * * * *")
    fun update() {
        val futures = liveStreamRepository.findAllOpenLiveStreams().map { liveStream ->
            CompletableFuture.supplyAsync({
                try {
                    chzzkLiveApiClient.fetchChzzkLiveDetail(liveStream.channelId!!)?.let { liveDetail ->
                        if (liveStream.liveId != liveDetail.liveId) {
                            liveStream.liveStatus = LiveStatus.CLOSE
                            liveStream.closedDate = LocalDateTime.now()
                            log.info(
                                "라이브 ID가 변경되었습니다. 기존 상태를 CLOSE로 업데이트합니다 - " +
                                        "채널 ID: ${liveStream.channelId}, " +
                                        "기존 라이브 ID: ${liveStream.liveId}, " +
                                        "새로운 라이브 ID: ${liveDetail.liveId}"
                            )
                        } else {
                            liveStream.update(
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
                                        "채널 ID: ${liveStream.channelId}, " +
                                        "상태: ${liveDetail.status}, " +
                                        "제목: ${liveDetail.liveTitle}, " +
                                        "카테고리: ${liveDetail.categoryType}, " +
                                        "시작 시간: ${liveDetail.openDate}, " +
                                        "종료 시간: ${liveDetail.closeDate}"
                            )
                        }
                        liveStream
                    }
                } catch (e: Exception) {
                    log.error("라이브 스트림 업데이트 중 오류 발생 - 채널 ID: ${liveStream.channelId}, 오류: ${e.message}", e)
                    null
                }
            }, executor)
        }
        
        val updatedLiveStreams = CompletableFuture.allOf(*futures.toTypedArray())
                .thenApply { futures.mapNotNull { it.join() } }
                .join()
        
        liveStreamRepository.batchUpdateLiveStreams(updatedLiveStreams)
        log.info("총 ${updatedLiveStreams.size}개의 라이브 스트림이 업데이트되었습니다.")
    }
}