package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.domain.exception.InvalidParticipantException
import com.chit.app.domain.session.domain.exception.SessionParticipantNotFoundException
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.sse.infrastructure.SseAdapter
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ParticipantService(
        private val sseAdapter: SseAdapter,
        private val sessionRepository: SessionRepository
) {
    
    private val log = logger<ParticipantService>()
    
    @Transactional
    fun leaveSession(sessionCode: String, viewerId: Long) {
        log.info("[요청] 세션 퇴장 요청 (viewerId={}, sessionCode={})", viewerId, sessionCode)
        val participant = sessionRepository.findParticipantBy(viewerId, sessionCode) ?: throw InvalidParticipantException()
        
        participant.leaveContentSession()
        sseAdapter.emitExitEventAsync(participant)
        log.info("[성공] 세션 퇴장 완료 (viewerId={}, sessionCode={})", viewerId, sessionCode)
    }
    
    @Transactional
    fun kickParticipant(streamerId: Long, viewerId: Long?) {
        log.info("[요청] 참여자 강퇴 요청 (streamerId={}, viewerId={})", streamerId, viewerId)
        require(viewerId != null) { "유효하지 않은 참여자 정보입니다." }
        val participant = sessionRepository.findParticipantBy(viewerId, streamerId = streamerId)
                ?.apply {
                    leaveContentSession()
                    log.info("[진행] 강퇴 대상 참여자 처리 완료 (participantId={}, sessionCode={})", id, contentsSession.sessionCode)
                }
                ?: throw SessionParticipantNotFoundException()
        
        sseAdapter.emitKickEventAsync(participant)
        log.info("[성공] 강퇴 SSE 이벤트 발송 완료 (participantId={}, sessionCode={})", participant.id, participant.contentsSession.sessionCode)
    }
    
    private fun SessionParticipant.leaveContentSession() {
        status = ParticipationStatus.LEFT
        contentsSession.removeParticipant()
    }
    
}