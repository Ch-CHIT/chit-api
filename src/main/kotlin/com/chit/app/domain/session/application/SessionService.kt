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
        val openLiveStream = getOpenLiveStream(streamerId)
        check(!sessionRepository.existsOpenContentsSession(openLiveStream.liveId!!)) {
            "이미 진행 중인 컨텐츠 세션이 존재합니다. 중복 생성을 할 수 없습니다."
        }
        
        val contentsSession = ContentsSession.create(
            liveId = openLiveStream.liveId,
            streamerId = streamerId,
            maxGroupParticipants = maxGroupParticipants,
            gameParticipationCode = gameParticipationCode
        )
        
        val savedSession = sessionRepository.save(contentsSession)
        return savedSession.toResponseDto()
    }
    
    @Transactional(readOnly = true)
    fun getCurrentOpeningContentsSession(streamerId: Long, pageable: Pageable): ContentsSessionResponseDto {
        val openContentsSession = getOpenContentsSessionByStreamerId(streamerId)
        val participants = sessionRepository.findPagedParticipantsBySessionCode(openContentsSession.sessionCode, pageable)
        
        return ContentsSessionResponseDto(
            sessionCode = openContentsSession.sessionCode,
            gameParticipationCode = openContentsSession.gameParticipationCode,
            maxGroupParticipants = openContentsSession.maxGroupParticipants,
            currentParticipants = openContentsSession.currentParticipants,
            participants = PagedResponse.from(participants)
        )
    }
    
    @Transactional
    fun updateContentsSession(streamerId: Long, maxGroupParticipants: Int, gameParticipationCode: String?): ContentsSessionResponseDto {
        val session = getOpenContentsSessionByStreamerId(streamerId)
        session.updateGameSettings(gameParticipationCode, maxGroupParticipants)
        
        return with(session.toResponseDto()) {
            streamerSseService.publishEvent(streamerId, SseEvent.SESSION_STATUS_UPDATED, this)
            this
        }
    }
    
    @Transactional
    fun removeParticipantFromSession(streamerId: Long, participantId: Long?) {
        require(participantId != null) { "유효하지 않은 참여자 정보입니다." }
        
        val session = getOpenContentsSessionByStreamerId(streamerId)
        val participant = sessionRepository.findParticipantBySessionIdAndParticipantId(session.id!!, participantId) ?: return
        
        session.removeParticipant()
        participant.updateStatus(status = ParticipationStatus.REJECTED)
        
        sessionSseService.disconnectParticipant(session.sessionCode, participantId)
        streamerSseService.publishEvent(streamerId, SseEvent.PARTICIPANT_REMOVED, session.toResponseDto())
        sessionSseService.reorderSessionParticipants(session.sessionCode, SseEvent.PARTICIPANT_REMOVED)
    }
    
    @Transactional
    fun closeContentsSession(streamerId: Long) = getOpenContentsSessionByStreamerId(streamerId)
            .apply {
                close()
                streamerSseService.unsubscribe(streamerId)
                sessionSseService.removeAllParticipants(this.sessionCode)
                log.info("세션 종료 처리 완료: 스트리머 ID = $streamerId, 참여 코드 = ${this.sessionCode}")
            }
    
    @Transactional
    fun togglePick(streamerId: Long, participantId: Long) {
        val session = getOpenContentsSessionByStreamerId(streamerId)
        sessionRepository.findParticipantBySessionIdAndParticipantId(session.id!!, participantId)
                ?.apply { toggleFixedPick() }
                ?: throw IllegalArgumentException("참여자 ID ${participantId}가 세션 ID ${session.id}에 존재하지 않습니다.")
        
        sessionSseService.reorderSessionParticipants(session.sessionCode, SseEvent.PARTICIPANT_UPDATED)
    }
    
    private fun getOpenContentsSessionByStreamerId(streamerId: Long): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(streamerId = streamerId)
                ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다.")
    }
    
    private fun getOpenLiveStream(streamerId: Long): LiveStream {
        return liveStreamRepository.findOpenLiveStreamBy(streamerId = streamerId)
                ?: throw IllegalArgumentException("현재 진행중인 라이브 방송을 찾을 수 없습니다. 다시 확인해 주세요.")
    }
    
    private fun ContentsSession.toResponseDto(): ContentsSessionResponseDto {
        return ContentsSessionResponseDto(
            sessionCode = sessionCode,
            gameParticipationCode = gameParticipationCode,
            maxGroupParticipants = maxGroupParticipants,
            currentParticipants = currentParticipants
        )
    }
    
}