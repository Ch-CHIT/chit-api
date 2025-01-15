package com.chit.app.domain.session.application

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.application.sse.SessionSseService
import com.chit.app.domain.session.application.sse.StreamerSseService
import com.chit.app.domain.session.domain.model.ContentsSession
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.global.delegate.logger
import com.chit.app.global.response.SuccessResponse.PagedResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SessionService(
        private val liveStreamRepository: LiveStreamRepository,
        private val sessionRepository: SessionRepository,
        private val streamerSseService: StreamerSseService,
        private val sessionSseService: SessionSseService
) {
    
    private val log = logger<SessionService>()
    
    @Transactional
    fun createContentsSession(streamerId: Long, maxGroupParticipants: Int, gameParticipationCode: String?): ContentsSessionResponseDto {
        val liveStream = getOpenLiveStream(streamerId)
        return sessionRepository.save(
            ContentsSession.create(
                liveId = liveStream.liveId,
                streamerId = streamerId,
                maxGroupParticipants = maxGroupParticipants,
                gameParticipationCode = gameParticipationCode
            )
        ).toResponseDto()
        log.info("새로운 참여 세션이 생성되었습니다. 세션 ID: ${savedSession.sessionId}, 라이브 ID: ${savedSession.liveId}")
        return savedSession
    }
    
    @Transactional(readOnly = true)
    fun getCurrentOpeningContentsSession(streamerId: Long, pageable: Pageable): ContentsSessionResponseDto {
        val session = sessionRepository.findOpenContentsSessionByStreamerId(streamerId) ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다.")
        val participants = sessionRepository.findPagedParticipantsBySessionCode(session.sessionCode, pageable)
        
        return ContentsSessionResponseDto(
            sessionCode = session.sessionCode,
            gameParticipationCode = session.gameParticipationCode,
            maxGroupParticipants = session.maxGroupParticipants,
            currentParticipants = session.currentParticipants,
            participants = PagedResponse.from(participants)
        )
    }
    
    @Transactional
    fun updateContentsSession(
            streamerId: Long,
            maxGroupParticipants: Int,
            gameParticipationCode: String?
    ): ContentsSessionResponseDto {
        val session = sessionRepository.findOpenContentsSessionByStreamerId(streamerId) ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다.")
        session.updateGameDetails(gameParticipationCode, maxGroupParticipants)
        
        return with(session.toResponseDto()) {
            streamerSseService.publishEvent(streamerId, SseEvent.SESSION_STATUS_UPDATED, this)
            this
        }
    }
    
    @Transactional
    fun removeParticipantFromSession(
            streamerId: Long,
            participantId: Long
    ) {
        val session = sessionRepository.findOpenContentsSessionByStreamerId(streamerId) ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다.")
        val participant = sessionRepository.findParticipantBySessionIdAndParticipantId(session.id, participantId) ?: return
        
        participant.updateStatus(status = ParticipationStatus.REJECTED)
        sessionSseService.disconnectParticipant(session.sessionCode, participantId)
        sessionSseService.updateAllParticipantsOrder(session.sessionCode, SseEvent.PARTICIPANT_REMOVED)
    }
    
    @Transactional
    fun closeContentsSession(streamerId: Long) {
        val session = sessionRepository.findOpenContentsSessionByStreamerId(streamerId) ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다.")
        session.apply {
            close()
            streamerSseService.unsubscribe(streamerId)
            sessionSseService.removeAllParticipants(sessionCode)
            log.info("세션 종료 처리 완료: 스트리머 ID = $streamerId, 참여 코드 = ${session.sessionCode}")
        }
    }
    
    @Transactional
    fun togglePick(streamerId: Long, participantId: Long) {
        val session = sessionRepository.findOpenContentsSessionByStreamerId(streamerId) ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다.")
        val participant = sessionRepository.findParticipantBySessionIdAndParticipantId(session.id, participantId) ?: throw IllegalArgumentException("찾을 수 없음")
        participant.toggleFixedPick()
        sessionSseService.updateAllParticipantsOrder(session.sessionCode, SseEvent.PARTICIPANT_UPDATED)
    }
    
    private fun getOpenLiveStream(streamerId: Long): LiveStream =
            liveStreamRepository.findOpenLiveStreamByStreamerId(streamerId)
                    ?.also { require(!sessionRepository.hasOpenContentsSession(it.liveId)) { "이미 라이브 방송에 시청자 참여 세션이 열려 있습니다." } }
                    ?: throw IllegalArgumentException("현재 진행중인 라이브 방송을 찾을 수 없습니다. 다시 확인해 주세요.")
    
    private fun ContentsSession.toResponseDto(): ContentsSessionResponseDto {
        return ContentsSessionResponseDto(
            sessionCode = sessionCode,
            gameParticipationCode = gameParticipationCode,
            maxGroupParticipants = maxGroupParticipants,
            currentParticipants = currentParticipants
        )
    }
    
}