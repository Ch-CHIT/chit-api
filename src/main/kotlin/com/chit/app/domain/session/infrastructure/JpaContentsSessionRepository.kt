package com.chit.app.domain.session.infrastructure

import com.chit.app.domain.session.domain.model.ContentsSession
import com.chit.app.domain.session.domain.model.status.SessionStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional

interface JpaContentsSessionRepository : JpaRepository<ContentsSession, Long> {
    
    @Query("""
        SELECT
            CASE WHEN COUNT(cs) > 0
              THEN TRUE
              ELSE FALSE
            END
          FROM ContentsSession cs
          WHERE cs.liveId = :liveId
            AND cs._status = :status
    """)
    fun existsByLiveIdAndStatus(
            @Param("liveId") liveId: Long?,
            @Param("status") status: SessionStatus
    ): Boolean
    
    @Query("""
        SELECT cs
        FROM ContentsSession cs
        WHERE cs.streamerId = :streamerId
          AND cs._status = :status
    """)
    fun findByStreamerIdAndStatus(
            @Param("streamerId") streamerId: Long,
            @Param("status") status: SessionStatus
    ): ContentsSession?
    
    @Query("""
        SELECT cs
        FROM ContentsSession cs
        WHERE cs.sessionCode = :sessionCode
    """)
    fun findBySessionCode(
            @Param("sessionCode") sessionCode: String
    ): ContentsSession?
    
    @Modifying
    @Transactional
    @Query("""
        UPDATE ContentsSession cs
        SET cs._status = 'CLOSE'
        WHERE cs._status = 'OPEN'
    """)
    fun closeAllOpenSessions(): Int
    
}