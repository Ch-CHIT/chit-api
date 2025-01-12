package com.chit.app.domain.session.domain.repository

import com.chit.app.domain.session.application.dto.Participant
import com.chit.app.domain.session.domain.model.ContentsSession
import com.chit.app.domain.session.domain.model.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.model.status.SessionStatus
import com.chit.app.domain.session.infrastructure.JpaContentsSessionRepository
import com.chit.app.domain.session.infrastructure.JpaSessionParticipantRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class SessionRepository(
        private val sessionRepository: JpaContentsSessionRepository,
        private val participantRepository: JpaSessionParticipantRepository,
) {
    
    fun save(session: ContentsSession): ContentsSession? =
            sessionRepository.save(session)
    
    fun addParticipant(sessionParticipant: SessionParticipant) =
            participantRepository.save(sessionParticipant).contentsSession.addParticipant()
    
    fun hasOpenContentsSession(liveId: Long): Boolean =
            sessionRepository.existsByLiveIdAndStatus(liveId, SessionStatus.OPEN)
    
    fun findOpenContentsSessionByStreamerId(streamerId: Long): ContentsSession? =
            sessionRepository.findByStreamerIdAndStatus(streamerId, SessionStatus.OPEN)
    
    fun findPagedParticipantsBySessionCode(sessionCode: String, pageable: Pageable): Page<Participant> =
            participantRepository.findActiveParticipantsBySessionCode(sessionCode, ParticipationStatus.REJECTED, pageable)
    
    fun findBySessionParticipationCode(sessionCode: String): ContentsSession? =
            sessionRepository.findBySessionCode(sessionCode)
    
}