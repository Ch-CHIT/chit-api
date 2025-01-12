package com.chit.app.domain.live.infrastructure

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.model.LiveStatus
import org.springframework.data.jpa.repository.JpaRepository

interface JpaLiveStreamRepository : JpaRepository<LiveStream, Long> {
    fun findByStreamerIdAndLiveStatusIs(streamerId: Long, liveStatus: LiveStatus): LiveStream?
    fun findByChannelIdAndLiveStatusIs(channelId: String, liveStatus: LiveStatus): LiveStream?
}