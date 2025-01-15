package com.chit.app.domain.live.application

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.live.infrastructure.ChzzkLiveApiClient
import com.chit.app.domain.live.infrastructure.ChzzkLiveApiClient.LiveDetailResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LiveStreamService(
        private val chzzkLiveApiClient: ChzzkLiveApiClient,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    @Transactional
    fun upsert(streamerId: Long?, channelId: String) {
        val liveDetail = chzzkLiveApiClient.fetchChzzkLiveDetail(channelId) ?: return
        val existingLiveStream = liveStreamRepository.findOpenLiveStreamByChannelId(channelId)
        
        if (existingLiveStream != null) {
            updateExistingLiveStream(existingLiveStream, liveDetail)
        } else {
            createNewLiveStream(streamerId, channelId, liveDetail)
        }
    }
    
    private fun updateExistingLiveStream(existingLiveStream: LiveStream, liveDetail: LiveDetailResponse.Content) {
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
    
    private fun createNewLiveStream(streamerId: Long?, channelId: String, liveDetail: LiveDetailResponse.Content) {
        val (liveId, liveTitle, status, categoryType, liveCategory, liveCategoryValue, openDate, closeDate) = liveDetail
        LiveStream.create(
            liveId = liveId,
            streamerId = streamerId,
            channelId = channelId,
            liveTitle = liveTitle,
            liveStatus = status,
            categoryType = categoryType,
            liveCategory = liveCategory,
            liveCategoryValue = liveCategoryValue,
            openDate = openDate,
            closeDate = closeDate
        ).also(liveStreamRepository::save)
    }
    
}