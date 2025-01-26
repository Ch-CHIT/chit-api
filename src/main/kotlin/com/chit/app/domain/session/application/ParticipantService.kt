package com.chit.app.domain.session.application

import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.application.sse.StreamerSseService
import com.chit.app.domain.session.domain.model.SessionParticipant
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.global.delegate.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ParticipantService(
        private val sessionRepository: SessionRepository,
        private val streamerSseService: StreamerSseService
) {
    
    private val log = logger<ParticipantService>()
    
    @Transactional
    fun joinSession(sessionCode: String, participantId: Long, gameNickname: String) {
        // 세션 코드에 해당하는 열린 세션을 조회
        val contentsSession = sessionRepository.findOpenContentsSessionBy(sessionCode = sessionCode) ?: run {
            throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
        }
        log.debug("세션 조회 성공 - sessionCode: $sessionCode, streamerId: ${contentsSession.streamerId}")
        
        // 새로운 참가자 생성 및 세션에 추가
        sessionRepository.addParticipant(
            SessionParticipant.create(
                participantId = participantId,
                gameNickname = gameNickname,
                contentsSession = contentsSession
            )
        )
        log.debug("참가자 추가 성공 - participantId: $participantId, gameNickname: $gameNickname, sessionCode: $sessionCode")
        
        // SSE 이벤트 발행
        streamerSseService.publishEvent(
            streamerId = contentsSession.streamerId,
            event = SseEvent.PARTICIPANT_ADDED,
            data = ContentsSessionResponseDto(
                maxGroupParticipants = contentsSession.maxGroupParticipants,
                currentParticipants = contentsSession.currentParticipants
            )
        )
    }
    
}