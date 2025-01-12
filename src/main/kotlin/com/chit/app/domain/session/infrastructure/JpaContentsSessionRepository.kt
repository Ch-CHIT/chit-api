package com.chit.app.domain.session.infrastructure

import com.chit.app.domain.session.domain.model.ContentsSession
import com.chit.app.domain.session.domain.model.status.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository

interface JpaContentsSessionRepository : JpaRepository<ContentsSession, Long> {
    fun existsByLiveIdAndStatus(liveId: Long, status: SessionStatus): Boolean
    fun findByStreamerIdAndStatus(streamerId: Long, status: SessionStatus): ContentsSession?
    fun findBySessionCode(sessionParticipationCode: String): ContentsSession?
}