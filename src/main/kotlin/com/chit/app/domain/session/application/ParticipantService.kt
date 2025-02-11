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
    suspend fun joinSession(
            sessionCode: String,
            participantId: Long,
            gameNickname: String
    ) {
        val contentsSession = sessionRepository.findOpenContentsSessionBy(sessionCode = sessionCode) ?: run {
            throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
        }
        
        sessionRepository.addParticipant(
            SessionParticipant.create(
                participantId = participantId,
                gameNickname = gameNickname,
                contentsSession = contentsSession
            )
        )
        
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