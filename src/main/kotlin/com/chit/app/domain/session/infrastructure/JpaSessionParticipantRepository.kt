package com.chit.app.domain.session.infrastructure

import com.chit.app.domain.session.application.dto.Participant
import com.chit.app.domain.session.domain.model.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface JpaSessionParticipantRepository : JpaRepository<SessionParticipant, Long> {
    
    @Query(
        value = """
            SELECT new com.chit.app.domain.session.application.dto.Participant(
                sp.participantId,
                m.channelName,
                sp._gameNickname,
                sp._fixedPick
            )
            FROM SessionParticipant sp
              JOIN sp.contentsSession cs
              JOIN Member m ON m.id = sp.participantId
            WHERE cs.sessionCode = :code
              AND sp._status != :status
            ORDER BY sp._fixedPick DESC,
                     sp._status ASC,
                     sp.id ASC
        """,
        countQuery = """
                   SELECT COUNT(sp)
                   FROM SessionParticipant sp
                    JOIN sp.contentsSession cs
                    JOIN Member m ON m.id = sp.participantId
                   WHERE cs.sessionCode = :code
                     AND sp._status != :status
               """
    )
    fun findActiveParticipantsBySessionCode(
            @Param("code") code: String,
            @Param("status") status: ParticipationStatus,
            pageable: Pageable
    ): Page<Participant>
    
    @Query(
        """
        SELECT sp
        FROM SessionParticipant sp
          JOIN FETCH sp.contentsSession cs
        WHERE cs.sessionCode = :code
          AND sp._status != :status
        ORDER BY sp._fixedPick DESC,
                 sp._status ASC,
                 sp.id ASC
        """
    )
    fun findSortedParticipantsBySessionCode(
            @Param("code") sessionCode: String,
            @Param("status") status: ParticipationStatus
    ): List<SessionParticipant>
    
    @Query(
        """
        SELECT sp
        FROM SessionParticipant sp
        WHERE sp.contentsSession.id = :sessionId AND sp.participantId = :participantId
        """
    )
    fun findParticipantBySessionIdAndParticipantId(
            @Param("sessionId") sessionId: Long?,
            @Param("participantId") participantId: Long
    ): SessionParticipant?
    
}