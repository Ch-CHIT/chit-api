package com.chit.app.domain.session.domain.repository

import com.chit.app.domain.session.application.dto.Participant
import com.chit.app.domain.session.domain.model.ContentsSession
import com.chit.app.domain.session.domain.model.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.model.status.SessionStatus
import com.chit.app.domain.session.infrastructure.JpaContentsSessionRepository
import com.chit.app.domain.session.infrastructure.JpaSessionParticipantRepository
import com.chit.app.global.handler.EntitySaveExceptionHandler
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class SessionRepository(
        private val sessionRepository: JpaContentsSessionRepository,
        private val participantRepository: JpaSessionParticipantRepository,
) {
    
    fun save(session: ContentsSession): ContentsSession =
            runCatching { sessionRepository.save(session) }
                    .onFailure { EntitySaveExceptionHandler.handle(it) }
                    .getOrThrow()
    
    fun hasOpenContentsSession(liveId: Long?): Boolean =
            sessionRepository.existsByLiveIdAndStatus(liveId, SessionStatus.OPEN)
    
    fun findBySessionCode(sessionCode: String): ContentsSession? =
            sessionRepository.findBySessionCode(sessionCode)
    
    fun findOpenContentsSessionByStreamerId(streamerId: Long): ContentsSession? =
            sessionRepository.findByStreamerIdAndStatus(streamerId, SessionStatus.OPEN)
    
    fun findPagedParticipantsBySessionCode(sessionCode: String, pageable: Pageable): Page<Participant> =
            participantRepository.findActiveParticipantsBySessionCode(sessionCode, ParticipationStatus.REJECTED, pageable)
    
    fun addParticipant(sessionParticipant: SessionParticipant) =
            participantRepository.save(sessionParticipant).contentsSession.addParticipant()
    
    fun findParticipantBySessionIdAndParticipantId(sessionId: Long?, participantId: Long): SessionParticipant? =
            participantRepository.findParticipantBySessionIdAndParticipantId(sessionId, participantId)
    
    fun findSortedParticipantsBySessionCode(sessionCode: String): List<SessionParticipant> =
            participantRepository.findSortedParticipantsBySessionCode(sessionCode, ParticipationStatus.REJECTED)
    
}