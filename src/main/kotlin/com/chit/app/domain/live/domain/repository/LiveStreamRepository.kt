package com.chit.app.domain.live.domain.repository

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.model.QLiveStream
import com.chit.app.domain.live.infrastructure.JpaLiveStreamRepository
import com.chit.app.global.handler.EntitySaveExceptionHandler
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Slice
import org.springframework.data.domain.SliceImpl
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
    
    fun findAllOpenLiveStreams(pageable: Pageable): Slice<LiveStream> {
        val results = query
                .selectFrom(liveStream)
                .where(liveStream._liveStatus.eq(LiveStatus.OPEN))
                .offset(pageable.offset)
                .limit(pageable.pageSize + 1L)
                .fetch()
        val hasNext = results.size > pageable.pageSize
        val content = if (hasNext) results.subList(0, pageable.pageSize) else results
        return SliceImpl(content, pageable, hasNext)
    }
    
}