package com.chit.app.domain.live.domain.repository

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.infrastructure.JpaLiveStreamRepository
import com.chit.app.global.handler.EntitySaveExceptionHandler
import org.springframework.stereotype.Repository

@Repository
class LiveStreamRepository(
        private val repository: JpaLiveStreamRepository
) {
    
    fun save(liveStream: LiveStream): LiveStream =
            runCatching { repository.save(liveStream) }
                    .onFailure { EntitySaveExceptionHandler.handle(it) }
                    .getOrThrow()
    
    
    fun findOpenLiveStreamByStreamerId(streamerId: Long): LiveStream? =
            repository.findByStreamerIdAndLiveStatus(streamerId, LiveStatus.OPEN)
    
    fun findOpenLiveStreamByChannelId(channelId: String): LiveStream? =
            repository.findByChannelIdAndLiveStatus(channelId, LiveStatus.OPEN)
    
}