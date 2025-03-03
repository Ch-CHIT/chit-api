package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.annotation.LogExecutionTime
import com.chit.app.global.common.logging.logger
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
    
    private val log = logger<ParticipantService>()
    
    /**
     * 스트리머의 특정 세션에 참여자를 추가하고, 이후 전체 참여자 순서를 재정렬
     *
     * 1. 주어진 sessionCode 로 열려 있는 세션을 조회
     * 2. 해당 세션에 viewerId가 참여 중이지 않으면, 참여자를 등록
     * 3. 참여자 등록 후, 전체 참여자 순서를 재정렬하여 클라이언트에 최신 순서를 전파
     */
    @LogExecutionTime
    @Transactional
    fun joinParticipant(sessionCode: String, viewerId: Long, gameNickname: String) {
        // 세션 코드에 해당하는 열려 있는 세션을 조회 (세션이 없으면 예외 발생)
        val session = findOpenSessionOrThrow(sessionCode)
        
        // 해당 세션에 참여자가 없으면 등록
        if (isViewerNotInSession(session.id!!, viewerId)) {
            registerParticipant(session, viewerId, gameNickname)
        }
        
        // 참여자 등록 후, 전체 참여자 순서를 재정렬
        reorderParticipants(session)
    }
    
    /**
     * 특정 세션에서 참여자를 제거하고, 이후 전체 참여자 순서를 재정렬
     *
     * 1. viewerId와 sessionCode로 참여자를 조회
     * 2. 참여자가 존재하면, cleanupAfterRemoval()을 호출하여 제거 후 후처리를 수행
     * 3. 참여자 제거 후, 해당 세션의 전체 참여자 순서를 재정렬
     */
    @LogExecutionTime
    @Transactional
    fun removeParticipant(sessionCode: String, viewerId: Long) {
        // 참여자 조회 후, cleanupAfterRemoval() 호출하여 제거 및 후처리 수행
        val participant = sessionRepository.findParticipantBy(viewerId, sessionCode)
                ?.apply { cleanupAfterRemoval() }
                ?: throw IllegalArgumentException("해당 세션 참여 정보를 확인할 수 없습니다. 다시 시도해 주세요.")
        
        // 참여자가 제거된 세션에 대해 순서 재정렬 진행
        reorderParticipants(participant.contentsSession)
    }
    
    /**
     * 참여자를 제거할 때, 후처리 작업(상태 업데이트, 세션 참여자 수 감소, SSE 연결 종료, 순서 재정렬, 이벤트 발송)
     */
    private fun SessionParticipant.cleanupAfterRemoval() {
        // 참여 상태 업데이트 -> 'LEFT'
        status = ParticipationStatus.LEFT
        
        // 세션 내 참여자 수 감소 (-1)
        contentsSession.removeParticipant()
        
        // SSE 연결 종료: 해당 세션의 emitter 연결 해제
        sessionSseService.disconnectSseEmitter(contentsSession.sessionCode, viewerId)
        
        // 참가 순서 재정렬
        ParticipantOrderManager.removeParticipantOrder(contentsSession.sessionCode, viewerId)
        
        // 스트리머 이벤트 발송: 참여자 제거 이벤트 알림 전송
        emitStreamerEvent(SseEvent.STREAMER_PARTICIPANT_REMOVED, contentsSession)
    }
    
    /**
     * 새로운 참여자를 생성하여 해당 세션에 등록
     *
     * 1. 신규 참여자 객체를 생성
     * 2. 참여자 객체를 저장하고, 세션 내 참여자 수를 증가
     * 3. 참여 순서를 추가 또는 업데이트
     * 4. 스트리머에게 참여자 추가 이벤트를 발송
     */
    private fun registerParticipant(session: ContentsSession, viewerId: Long, gameNickname: String) {
        // 신규 참여자 객체 생성
        val participant = SessionParticipant.create(viewerId, gameNickname, session)
        
        // 참여자 저장 및 세션 내 참여자 수 증가 (+1) 및 참여 순서 추가 또는 업데이트
        sessionRepository.addParticipant(participant)
                .apply { contentsSession.addParticipant() }
                .also { ParticipantOrderManager.addOrUpdateParticipantOrder(session.sessionCode, participant, viewerId) }
        
        // 스트리머 이벤트 발송: 신규 참여자 추가 이벤트 알림 전송
        emitStreamerEvent(SseEvent.STREAMER_PARTICIPANT_ADDED, session)
    }
    
    /**
     * 주어진 세션 코드를 기반으로 열려 있는 세션 정보를 조회
     */
    private fun findOpenSessionOrThrow(sessionCode: String): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(sessionCode)
                ?: throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
    }
    
    /**
     * 주어진 세션 ID와 참여자 ID를 기반으로, 참여자가 세션에 포함되어 있지 않음을 확인
     *
     * @return 참여자가 세션에 없으면 true, 있으면 false 반환
     */
    private fun isViewerNotInSession(sessionId: Long, viewerId: Long): Boolean {
        return !sessionRepository.existsParticipantInSession(sessionId, viewerId)
    }
    
    /**
     * 스트리머 이벤트 발송을 위한 헬퍼 메서드
     * - 비동기로 스트리머에게 이벤트를 전달
     */
    private fun emitStreamerEvent(event: SseEvent, session: ContentsSession) {
        runAsync({
            val data = mapOf(
                "maxGroupParticipants" to session.maxGroupParticipants,
                "currentParticipants" to session.currentParticipants
            )
            streamerSseService.emitStreamerEvent(event, session.streamerId, data)
        }, taskExecutor)
    }
    
    /**
     * 전체 참여자 순서를 재정렬
     * - 비동기로 세션 내 모든 참여자에게 순서 업데이트 이벤트를 전송
     */
    private fun reorderParticipants(session: ContentsSession) {
        runAsync({
            sessionSseService.reorderSessionParticipants(
                SseEvent.PARTICIPANT_ORDER_UPDATED,
                session.sessionCode,
                session.gameParticipationCode,
                session.maxGroupParticipants
            )
        }, taskExecutor)
    }
    
}