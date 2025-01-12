package com.chit.app.domain.live.domain.repository

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.infrastructure.JpaLiveStreamRepository
import org.springframework.stereotype.Repository

@Repository
class LiveStreamRepository(
        private val repository: JpaLiveStreamRepository
) {
    
    fun findOpenLiveStreamByStreamerId(streamerId: Long): LiveStream? =
            repository.findByStreamerIdAndLiveStatus(streamerId, LiveStatus.OPEN)
    
    fun findOpenLiveStreamByChannelId(channelId: String): LiveStream? =
            repository.findByChannelIdAndLiveStatus(channelId, LiveStatus.OPEN)
    
}