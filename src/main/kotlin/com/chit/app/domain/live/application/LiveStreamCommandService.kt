package com.chit.app.domain.live.application

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.live.infrastructure.ChzzkLiveApiClient
import com.chit.app.domain.live.infrastructure.response.LiveDetailResponse
import com.chit.app.domain.member.application.MemberQueryService
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service

@Service
class LiveStreamCommandService(
        private val chzzkLiveApiClient: ChzzkLiveApiClient,
        private val memberQueryService: MemberQueryService,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    private val log = logger<LiveStreamCommandService>()
    
    fun saveOrUpdate(streamerId: Long): LiveStream {
        // 1. 채널 ID 조회
        val channelId = memberQueryService.getMember(memberId = streamerId).channelId
        log.info("채널 ID 조회 완료: streamerId={}, channelId={}", streamerId, channelId)
        
        val liveDetail = chzzkLiveApiClient.fetchChzzkLiveDetail(channelId)
        log.info("라이브 정보 조회 완료: liveId={}", liveDetail.liveId)
        
        val latest = liveStreamRepository.findLatestLiveStreamBy(channelId = channelId)
        if (latest == null) {
            log.info("이전 라이브 스트림 없음: channelId={}", channelId)
        } else {
            log.info("이전 라이브 스트림 있음: liveId={}", latest.liveId)
        }
        
        return when {
            // 2. 라이브 정보 자체가 없다면 새로 생성
            latest == null                     -> {
                log.info("이전 기록이 없어 새로운 LiveStream 생성")
                create(streamerId, channelId, liveDetail)
            }
            
            // 3. liveId가 다르면 이전 스트림 종료 후 새로 생성
            liveDetail.liveId != latest.liveId -> {
                log.info("liveId 변경 감지: 이전={}, 신규={}, 이전 스트림 종료 후 새로 생성", latest.liveId, liveDetail.liveId)
                latest.liveStatus = LiveStatus.CLOSE
                liveStreamRepository.save(latest)
                create(streamerId, channelId, liveDetail)
            }
            
            // 4. 그 외엔 기존 스트림 업데이트
            else                               -> {
                log.info("동일 liveId, 기존 스트림 정보 업데이트")
                update(latest, liveDetail)
            }
        }
    }
    
    private fun create(streamerId: Long?, channelId: String, fetchedLiveDetail: LiveDetailResponse.Content): LiveStream {
        return LiveStream.create(
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
    
    private fun update(existingLiveStream: LiveStream, liveDetail: LiveDetailResponse.Content): LiveStream {
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
        return existingLiveStream
    }
    
}