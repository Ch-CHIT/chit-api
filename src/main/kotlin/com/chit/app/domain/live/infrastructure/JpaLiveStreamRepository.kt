package com.chit.app.domain.live.infrastructure

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.model.LiveStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface JpaLiveStreamRepository : JpaRepository<LiveStream, Long> {
    
    @Query("SELECT ls FROM LiveStream ls WHERE ls.channelId = :channelId AND ls._liveStatus = :liveStatus")
    fun findByChannelIdAndLiveStatus(
            @Param("channelId") channelId: String,
            @Param("liveStatus") liveStatus: LiveStatus
    ): LiveStream?
    
    @Query("SELECT ls FROM LiveStream ls WHERE ls.streamerId = :streamerId AND ls._liveStatus = :liveStatus")
    fun findByStreamerIdAndLiveStatus(
            @Param("streamerId") streamerId: Long,
            @Param("liveStatus") liveStatus: LiveStatus
    ): LiveStream?
    
}