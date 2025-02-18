package com.chit.app.domain.session.application.service

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.ParticipantOrder
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.delegate.logger
import com.chit.app.global.response.SuccessResponse.PagedResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

@Service
class SessionService(
        private val taskExecutor: ExecutorService,
        private val sessionSseService: SessionSseService,
        private val streamerSseService: StreamerSseService,
        private val liveStreamRepository: LiveStreamRepository,
        private val sessionRepository: SessionRepository,
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
        return session.updateGameSettings(gameParticipationCode, maxGroupParticipants).toResponseDto()
    }
    
    @Transactional
    fun processParticipantRemoval(streamerId: Long, viewerId: Long?) {
        // 1. viewerId 유효성 검증
        require(viewerId != null) { "유효하지 않은 참여자 정보입니다." }
        
        // 2. 스트리머의 오픈된 세션 조회
        val session = getOpenContentsSessionByStreamerId(streamerId)
        
        // 3. 세션 내에서 viewerId에 해당하는 참가자 조회 (없으면 예외 발생)
        val participant = sessionRepository.findParticipantBy(viewerId, sessionId = session.id!!)
                ?: throw IllegalArgumentException("참여자를 찾을 수 없습니다. 해당 세션에 유효한 참가자가 존재하지 않습니다.")
        
        // 4. 참가자 상태 업데이트 및 세션에서 제거 처리
        participant.status = ParticipationStatus.REJECTED
        session.removeParticipant()
        
        // 5. ParticipantOrder 업데이트 및 SSE 연결 해제
        ParticipantOrderManager.removeParticipant(session.sessionCode, viewerId)
        CompletableFuture.runAsync({
            sessionSseService.disconnectParticipant(session.sessionCode, viewerId)
        }, taskExecutor).thenRun {
            sessionSseService.reorderSessionParticipants(session.sessionCode, SseEvent.PARTICIPANT_ORDER_UPDATED)
        }.thenRun {
            streamerSseService.emitStreamerEvent(
                streamerId,
                SseEvent.STREAMER_PARTICIPANT_REMOVED,
                mapOf(
                    "maxGroupParticipants" to session.maxGroupParticipants,
                    "currentParticipants" to session.currentParticipants
                )
            )
        }
    }
    
    @Transactional
    fun closeContentsSession(streamerId: Long) {
        val session = getOpenContentsSessionByStreamerId(streamerId).apply { close() }
        sessionRepository.setAllParticipantsToLeft(session.sessionCode)
        streamerSseService.unsubscribe(streamerId)
        sessionSseService.disconnectAllParticipants(session.sessionCode)
        ParticipantOrderManager.removeSession(session.sessionCode)
    }
    
    @Transactional
    fun togglePick(streamerId: Long, viewerId: Long?) {
        // 1. 유효한 viewerId 검증
        require(viewerId != null) { "유효하지 않은 참여자 정보입니다." }
        
        // 2. 스트리머의 현재 오픈된 컨텐츠 세션 조회
        val session = getOpenContentsSessionByStreamerId(streamerId)
        
        // 3. 해당 세션 내 viewerId에 해당하는 참가자 조회 (없으면 예외 발생)
        val participant = sessionRepository.findParticipantBy(viewerId, sessionId = session.id!!)
                ?: throw IllegalArgumentException("참여자를 찾을 수 없습니다. 해당 세션에 유효한 참가자가 존재하지 않습니다.")
        
        // 4. 참가자의 고정 선택 상태 토글
        participant.toggleFixedPick()
        
        // 5. 변경된 참가자 정보를 기반으로 최신 순서 업데이트
        ParticipantOrderManager.updateParticipant(
            session.sessionCode,
            ParticipantOrder(
                fixed = participant.fixedPick,
                status = participant.status,
                participantId = participant.id!!,
                viewerId = viewerId
            )
        )
        
        // 6. 순서 재정렬 이벤트를 발행하여 클라이언트에게 최신 정보를 전달
        sessionSseService.reorderSessionParticipants(session.sessionCode, SseEvent.PARTICIPANT_ORDER_UPDATED)
    }
    
    @Transactional(readOnly = true)
    fun getGameParticipationCode(sessionCode: String, participantId: Long): ContentsSessionResponseDto {
        val gameParticipationCode = sessionRepository.findGameParticipationCodeBy(sessionCode, participantId)
                ?: throw IllegalArgumentException("해당 세션에 참가자 정보가 존재하지 않습니다. 다시 확인해 주세요.")
        return ContentsSessionResponseDto(gameParticipationCode = gameParticipationCode)
    }
    
    @Transactional
    fun leaveSession(viewerId: Long, sessionCode: String) {
        val participant = sessionRepository.findParticipantBy(viewerId, sessionCode = sessionCode) ?: return
        participant.status = ParticipationStatus.LEFT
        sessionSseService.disconnectParticipant(sessionCode, viewerId)
        sessionSseService.reorderSessionParticipants(sessionCode, SseEvent.PARTICIPANT_ORDER_UPDATED)
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