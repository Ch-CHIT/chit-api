package com.chit.app.domain.live.domain.repository

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.model.QLiveStream
import com.chit.app.domain.live.infrastructure.JpaLiveStreamRepository
import com.chit.app.global.handler.EntitySaveExceptionHandler
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class LiveStreamRepository(
        private val query: JPAQueryFactory,
        private val repository: JpaLiveStreamRepository
) {
    
    private val liveStream: QLiveStream = QLiveStream.liveStream
    
    fun save(liveStream: LiveStream): LiveStream =
            runCatching { repository.save(liveStream) }
                    .onFailure { EntitySaveExceptionHandler.handle(it) }
                    .getOrThrow()
    
    fun batchUpdateLiveStreams(liveStreams: List<LiveStream>) = repository.saveAll(liveStreams)
    
    fun findOpenLiveStreamBy(
            streamerId: Long? = null,
            channelId: String? = null,
    ): LiveStream? = query
            .selectFrom(liveStream)
            .where(
                streamerId?.let { liveStream.streamerId.eq(it) },
                channelId?.let { liveStream.channelId.eq(it) },
                liveStream._liveStatus.eq(LiveStatus.OPEN)
            )
            .fetchOne()
    
    fun findAllOpenLiveStreams(): List<LiveStream> = query
            .selectFrom(liveStream)
            .where(liveStream._liveStatus.eq(LiveStatus.OPEN))
            .fetch()
    
}