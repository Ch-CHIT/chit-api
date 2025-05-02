package com.chit.app.domain.live.application

import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.global.common.annotation.LogExecutionTime
import com.chit.app.global.common.logging.logger
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture.allOf
import java.util.concurrent.CompletableFuture.supplyAsync
import java.util.concurrent.ExecutorService

@Component
class LiveStreamScheduler(
        private val taskExecutor: ExecutorService,
        private val liveStreamSyncService: LiveStreamSyncService,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    private val log = logger<LiveStreamScheduler>()
    
    companion object {
        private const val SLICE_SIZE = 20
    }
    
    @LogExecutionTime
    @Transactional(readOnly = true)
    @Scheduled(cron = "0 0 * * * *")
    fun refreshLiveStreamStatus() {
        var page = 0
        var totalUpdated = 0
        var hasNext = true
        
        while (hasNext) {
            val slice = liveStreamRepository.findAllOpenLiveStreams(PageRequest.of(page, SLICE_SIZE))
            if (slice.content.isEmpty()) break
            
            val updateTasks = slice.content.map { liveStream ->
                supplyAsync({ liveStreamSyncService.syncLiveStreamStatus(liveStream) }, taskExecutor)
            }
            
            allOf(*updateTasks.toTypedArray()).join()
            val updatedStreams = updateTasks.mapNotNull { it.getNow(null) }
            
            totalUpdated += updatedStreams.size
            hasNext = slice.hasNext()
            page++
        }
        
        log.debug("LiveStream 업데이트 완료: 총 {}개 처리됨.", totalUpdated)
    }
    
}