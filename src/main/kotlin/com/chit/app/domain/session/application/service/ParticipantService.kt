package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.ParticipantOrder
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.delegate.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService

@Service
class ParticipantService(
        private val taskExecutor: ExecutorService,
        private val sessionRepository: SessionRepository,
        private val sessionSseService: SessionSseService,
        private val streamerSseService: StreamerSseService,
) {
    
    private val log = logger<ParticipantService>()
    
    @Transactional
    fun joinParticipantToSession(
            sessionCode: String,
            viewerId: Long,
            gameNickname: String
    ) {
        // Step 1: 유효한 오픈 세션 조회
        val session = sessionRepository.findOpenContentsSessionBy(sessionCode)
                ?: throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
        
        // Step 2: 참가자 추가 여부 확인
        if (!sessionRepository.existsParticipantInSession(session.id!!, viewerId)) {
            log.debug("세션 '{}'에 viewerId '{}'가 존재하지 않음. 참가자 추가 진행", session.id, viewerId)
            
            // 2a. 새 참가자 생성 및 등록
            val participant = SessionParticipant.create(viewerId, gameNickname, session)
            sessionRepository.addParticipant(participant)
            log.debug("새 참가자 등록 완료: participant id '{}'", participant.id)
            
            // 2b. 참가자 정렬 순서 정보 등록
            val participantOrder = ParticipantOrder(
                fixed = false,
                status = participant.status,
                participantId = participant.id!!,
                viewerId = viewerId
            )
            ParticipantOrderManager.addParticipant(sessionCode, participantOrder)
            
            // 2c. 스트리머에게 신규 참가자 추가 이벤트 비동기 전송
            CompletableFuture.runAsync({
                streamerSseService.emitStreamerEvent(
                    session.streamerId,
                    SseEvent.STREAMER_PARTICIPANT_ADDED,
                    mapOf(
                        "maxGroupParticipants" to session.maxGroupParticipants,
                        "currentParticipants" to session.currentParticipants
                    )
                )
            }, taskExecutor)
        } else {
            log.debug("세션 '{}'에 viewerId '{}'가 이미 참가 중임", session.id, viewerId)
        }
        
        // Step 3: 항상 참가자 재정렬 이벤트를 비동기 전송
        CompletableFuture.runAsync({
            sessionSseService.reorderSessionParticipants(sessionCode, SseEvent.PARTICIPANT_ORDER_UPDATED)
        }, taskExecutor)
    }
    
    @Transactional
    fun rejectParticipant(sessionCode: String, viewerId: Long) {
        // Step 1: 참가자 조회
        val participant = sessionRepository.findParticipantBy(viewerId, sessionCode)
        if (participant == null) {
            log.error("세션 {}에서 참가자 {}을(를) 찾을 수 없습니다.", sessionCode, viewerId)
            return
        }
        
        // Step 2: 참가자 상태를 REJECTED로 변경하고 세션에서 제거
        participant.status = ParticipationStatus.REJECTED
        participant.contentsSession.removeParticipant()
        
        // Step 3: 로그 출력
        log.debug("참가자 {}의 상태를 REJECTED로 업데이트하고 세션 {}에서 제거했습니다.", viewerId, sessionCode)
    }
    
}