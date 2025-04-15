package com.chit.app.domain.live.application

import com.chit.app.domain.live.domain.model.LiveStatus
import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.live.infrastructure.ChzzkLiveApiClient
import com.chit.app.domain.live.infrastructure.response.LiveDetailResponse
import com.chit.app.domain.member.application.MemberQueryService
import org.springframework.stereotype.Service

@Service
class LiveStreamCommandService(
        private val chzzkLiveApiClient: ChzzkLiveApiClient,
        private val memberQueryService: MemberQueryService,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    /**
     * 라이브 스트림 - 저장 또는 업데이트 처리 메서드
     *
     * 이 메서드는 주어진 스트리머 ID에 대해, 해당 스트리머의 채널 ID를 조회한 후,
     * 외부 API(chzzkLiveApiClient)를 통해 최신 라이브 스트림 상세 정보를 가져옵니다.
     * 이어서, 해당 채널에 기존 라이브 스트림 정보가 있는지 확인합니다.
     *
     * - 기존 라이브 스트림 정보가 없거나, 새로 가져온 라이브 상세 정보의 liveId가 기존의 liveId와 다른 경우:
     *   - 기존의 라이브 스트림이 존재한다면, 해당 스트림의 상태를 [LiveStatus.CLOSE]로 업데이트하고,
     *     새로운 라이브 스트림을 생성합니다.
     * - 그렇지 않으면(즉, 최신 정보가 동일한 경우) 기존 라이브 스트림 정보를 업데이트합니다.
     *
     * @param streamerId 스트리머의 고유 ID.
     * @return 저장 또는 업데이트된 [LiveStream] 객체.
     */
    fun saveOrUpdate(streamerId: Long): LiveStream {
        val channelId = memberQueryService.getMember(memberId = streamerId).channelId
        val fetchedLiveDetail = chzzkLiveApiClient.fetchChzzkLiveDetail(channelId)
        val latestLiveStream = liveStreamRepository.findLatestLiveStreamBy(channelId = channelId)
        
        // 라이브 스트림 정보가 없거나, 가져온 liveId와 기존 liveId가 다르면 새로운 스트림 생성
        if (latestLiveStream == null || fetchedLiveDetail.liveId != latestLiveStream.liveId) {
            latestLiveStream?.liveStatus = LiveStatus.CLOSE
            return create(streamerId, channelId, fetchedLiveDetail)
        } else {
            return update(latestLiveStream, fetchedLiveDetail)
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