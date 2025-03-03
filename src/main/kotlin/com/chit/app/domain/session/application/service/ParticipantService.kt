package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.annotation.LogExecutionTime
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.ExecutorService

@Service
class ParticipantService(
        private val taskExecutor: ExecutorService,
        private val sessionRepository: SessionRepository,
        private val sessionSseService: SessionSseService,
        private val streamerSseService: StreamerSseService,
) {
    
    @LogExecutionTime
    @Transactional
    fun joinParticipant(sessionCode: String, viewerId: Long, gameNickname: String) {
        val session = findOpenSessionOrThrow(sessionCode)
        if (isViewerNotInSession(session.id!!, viewerId)) {
            registerParticipant(session, viewerId, gameNickname)
        }
        
        reorderParticipants(session)
    }
    
    @LogExecutionTime
    @Transactional
    fun removeParticipant(sessionCode: String, viewerId: Long) {
        val participant = sessionRepository.findParticipantBy(viewerId, sessionCode)
                ?.apply { cleanupAfterRemoval() }
                ?: throw IllegalArgumentException("해당 세션 참여 정보를 확인할 수 없습니다. 다시 시도해 주세요.")
        
        reorderParticipants(participant.contentsSession)
    }
    
    private fun SessionParticipant.cleanupAfterRemoval() {
        // 1. 참여 상태 업데이트: 상태를 'LEFT'로 변경
        status = ParticipationStatus.LEFT
        
        // 2. 세션 내 참여자 수 감소: 해당 세션의 참여자 수 업데이트
        contentsSession.removeParticipant()
        
        // 3. SSE 연결 종료: 해당 세션의 emitter 연결 해제
        sessionSseService.disconnectSseEmitter(contentsSession.sessionCode, viewerId)
        
        // 4. 참가 순서 재정렬: 제거된 참여자에 대해 순서 재계산
        ParticipantOrderManager.removeParticipantOrder(contentsSession.sessionCode, viewerId)
        
        // 5. 스트리머 이벤트 발송: 참여자 제거 이벤트 알림 전송
        emitStreamerEvent(SseEvent.STREAMER_PARTICIPANT_REMOVED, contentsSession)
    }
    
    private fun registerParticipant(session: ContentsSession, viewerId: Long, gameNickname: String) {
        // 1. 신규 참여자 객체 생성
        val participant = SessionParticipant.create(viewerId, gameNickname, session)
        
        // 2. 참여자 저장 및 세션 내 참여자 수 업데이트
        sessionRepository.addParticipant(participant).apply {
            contentsSession.addParticipant()
        }
        
        // 3. 참여 순서 추가 또는 업데이트: 참여자의 순서를 관리
        ParticipantOrderManager.addOrUpdateParticipantOrder(session.sessionCode, participant, viewerId)
        
        // 4. 스트리머 이벤트 발송: 신규 참여자 추가 이벤트 알림 전송
        emitStreamerEvent(SseEvent.STREAMER_PARTICIPANT_ADDED, session)
    }
    
    private fun findOpenSessionOrThrow(sessionCode: String): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(sessionCode)
                ?: throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
    }
    
    private fun isViewerNotInSession(sessionId: Long, viewerId: Long): Boolean {
        return !sessionRepository.existsParticipantInSession(sessionId, viewerId)
    }
    
    private fun emitStreamerEvent(event: SseEvent, session: ContentsSession) {
        runAsync({
            val data = mapOf(
                "maxGroupParticipants" to session.maxGroupParticipants,
                "currentParticipants" to session.currentParticipants
            )
            streamerSseService.emitStreamerEvent(session.streamerId, data, event)
        }, taskExecutor)
    }
    
    private fun reorderParticipants(session: ContentsSession) {
        runAsync({
            sessionSseService.reorderSessionParticipants(
                session.sessionCode,
                session.gameParticipationCode,
                session.maxGroupParticipants,
                SseEvent.PARTICIPANT_ORDER_UPDATED
            )
        }, taskExecutor)
    }
    
}