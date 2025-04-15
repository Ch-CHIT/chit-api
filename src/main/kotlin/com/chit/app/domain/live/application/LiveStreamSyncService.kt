package com.chit.app.domain.live.application

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.live.infrastructure.ChzzkLiveApiClient
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime.now

@Service
class LiveStreamSyncService(
        private val chzzkLiveApiClient: ChzzkLiveApiClient,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    private val log = logger<LiveStreamSyncService>()
    
    @Transactional
    fun syncLiveStreamStatus(liveStream: LiveStream): LiveStream? {
        return chzzkLiveApiClient.fetchChzzkLiveDetail(liveStream.channelId!!).let { fetchLiveDetail ->
            if (liveStream.liveId != fetchLiveDetail.liveId) {
                liveStream.apply {
                    liveStatus = LiveStatus.CLOSE
                    closedDate = now()
                }
                log.debug("라이브 스트림 종료 처리 - 채널 ID: {}, 기존 라이브 ID: {}", liveStream.channelId, liveStream.liveId)
            } else {
                val (_, liveTitle, status, categoryType, liveCategory, liveCategoryValue, openDate, closeDate) = fetchLiveDetail
                liveStream.update(
                    liveTitle = liveTitle,
                    liveStatus = status,
                    categoryType = categoryType,
                    liveCategory = liveCategory,
                    liveCategoryValue = liveCategoryValue,
                    openDate = openDate,
                    closeDate = closeDate
                )
                log.debug("라이브 스트림 정보 동기화 완료 - 채널 ID: {}, 라이브 정보: {}", liveStream.channelId, fetchLiveDetail.toString())
            }
            
            liveStreamRepository.save(liveStream)
        }
    }
    
}