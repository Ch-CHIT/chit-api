package com.chit.app.domain.session.application.service

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.ParticipantOrder
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.event.ParticipantDisconnectionEvent
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.common.logging.logger
import com.chit.app.global.common.response.SuccessResponse.PagedResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.ExecutorService

@Service
class SessionService(
        private val taskExecutor: ExecutorService,
        private val eventPublisher: ApplicationEventPublisher,
        
        private val sessionSseService: SessionSseService,
        private val streamerSseService: StreamerSseService,
        
        private val sessionRepository: SessionRepository,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    private val log = logger<SessionService>()
    
    @Transactional
    fun createContentsSession(streamerId: Long, gameParticipationCode: String?, maxGroupParticipants: Int): ContentsSessionResponseDto {
        val openLiveStream = getOpenLiveStream(streamerId)
        check(!sessionRepository.existsOpenContentsSession(openLiveStream.liveId!!)) {
            "이미 진행 중인 컨텐츠 세션이 존재합니다. 중복 생성을 할 수 없습니다."
        }
        
        val contentsSession = ContentsSession.create(
            streamerId = streamerId,
            liveId = openLiveStream.liveId,
            maxGroupParticipants = maxGroupParticipants,
            gameParticipationCode = gameParticipationCode
        )
        val savedSession = sessionRepository.save(contentsSession)
        return savedSession.toResponseDto()
    }
    
    @Transactional(readOnly = true)
    fun getCurrentOpeningContentsSession(streamerId: Long, pageable: Pageable): ContentsSessionResponseDto {
        val session = getOpenContentsSessionByStreamerId(streamerId)
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
    fun updateContentsSession(streamerId: Long, maxGroupParticipants: Int, gameParticipationCode: String?): ContentsSessionResponseDto {
        val session = getOpenContentsSessionByStreamerId(streamerId)
        val updatedSession = session.updateGameSettings(gameParticipationCode, maxGroupParticipants)
        return updatedSession.toResponseDto()
    }
    
    @Transactional
    fun processParticipantRemoval(streamerId: Long, viewerId: Long?) {
        require(viewerId != null) { "유효하지 않은 참여자 정보입니다." }
        val session = getOpenContentsSessionByStreamerId(streamerId)
        eventPublisher.publishEvent(ParticipantDisconnectionEvent(session.sessionCode, viewerId))
    }
    
    @Transactional
    fun closeContentsSession(streamerId: Long) {
        val session = getOpenContentsSessionByStreamerId(streamerId).apply { close() }
        val sessionCode = session.sessionCode
        
        sessionRepository.setAllParticipantsToLeft(sessionCode)
        ParticipantOrderManager.removeSession(sessionCode)
        runAsync({
            sessionSseService.disconnectAllParticipants(sessionCode)
            streamerSseService.unsubscribe(streamerId)
        }, taskExecutor)
    }
    
    @Transactional
    fun togglePick(streamerId: Long, viewerId: Long?) {
        require(viewerId != null) { "유효하지 않은 참여자 정보입니다." }
        
        val session = getOpenContentsSessionByStreamerId(streamerId)
        val participant = sessionRepository.findParticipantBy(viewerId, sessionId = session.id!!)
                ?: throw IllegalArgumentException("참여자를 찾을 수 없습니다. 해당 세션에 유효한 참가자가 존재하지 않습니다.")
        
        participant.toggleFixedPick()
        val participantOrder = ParticipantOrder(
            fixed = participant.fixedPick,
            status = participant.status,
            participantId = participant.id!!,
            viewerId = viewerId
        )
        ParticipantOrderManager.updateParticipant(session.sessionCode, participantOrder)
        runAsync({ sessionSseService.reorderSessionParticipants(session.sessionCode, SseEvent.PARTICIPANT_ORDER_UPDATED) }, taskExecutor)
    }
    
    @Transactional(readOnly = true)
    fun getGameParticipationCode(sessionCode: String, participantId: Long): ContentsSessionResponseDto {
        val gameParticipationCode = sessionRepository.findGameParticipationCodeBy(sessionCode, participantId)
                ?: throw IllegalArgumentException("해당 세션에 참가자 정보가 존재하지 않습니다. 다시 확인해 주세요.")
        return ContentsSessionResponseDto(gameParticipationCode = gameParticipationCode)
    }
    
    @Transactional
    fun leaveSession(viewerId: Long, sessionCode: String) {
        sessionSseService.disconnectParticipant(sessionCode, viewerId)
    }
    
    private fun getOpenContentsSessionByStreamerId(streamerId: Long): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(streamerId = streamerId)
                ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다. 다시 확인해 주세요.")
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