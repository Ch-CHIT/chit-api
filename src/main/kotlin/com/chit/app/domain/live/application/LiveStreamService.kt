package com.chit.app.domain.live.application

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.live.infrastructure.ChzzkLiveApiClient
import com.chit.app.domain.live.infrastructure.response.LiveDetailResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LiveStreamService(
        private val chzzkLiveApiClient: ChzzkLiveApiClient,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    @Transactional
    fun saveOrUpdateLiveStream(streamerId: Long?, channelId: String) {
        val fetchedLiveDetail = chzzkLiveApiClient.fetchChzzkLiveDetail(channelId) ?: return
        val existingLiveStream = liveStreamRepository.findOpenLiveStreamBy(channelId = channelId)
                ?: run {
                    // 기존 라이브 스트림이 없으면 새로 생성
                    createLiveStream(streamerId, channelId, fetchedLiveDetail)
                    return
                }
        
        if (fetchedLiveDetail.liveId != existingLiveStream.liveId) {
            // 라이브 ID가 다르면 기존 스트림을 닫고 새 스트림 생성
            existingLiveStream.liveStatus = LiveStatus.CLOSE
            createLiveStream(streamerId, channelId, fetchedLiveDetail)
        } else {
            // 라이브 ID가 같으면 업데이트
            updateLiveStream(existingLiveStream, fetchedLiveDetail)
        }
    }
    
    private fun updateLiveStream(existingLiveStream: LiveStream, liveDetail: LiveDetailResponse.Content) {
        val (_, liveTitle, status, categoryType, liveCategory, liveCategoryValue, openDate, closeDate) = liveDetail
        existingLiveStream.update(
            liveTitle = liveTitle,
            liveStatus = status,
            categoryType = categoryType,
            liveCategory = liveCategory,
            liveCategoryValue = liveCategoryValue,
            openDate = openDate,
            closeDate = closeDate
        )
    }
    
    private fun createLiveStream(streamerId: Long?, channelId: String, fetchedLiveDetail: LiveDetailResponse.Content) {
        LiveStream.create(
            streamerId = streamerId,
            channelId = channelId,
            liveId = fetchedLiveDetail.liveId,
            liveTitle = fetchedLiveDetail.liveTitle,
            liveStatus = fetchedLiveDetail.status,
            categoryType = fetchedLiveDetail.categoryType,
            liveCategory = fetchedLiveDetail.liveCategory,
            liveCategoryValue = fetchedLiveDetail.liveCategoryValue,
            openDate = fetchedLiveDetail.openDate,
            closeDate = fetchedLiveDetail.closeDate
        ).also(liveStreamRepository::save)
    }
    
}