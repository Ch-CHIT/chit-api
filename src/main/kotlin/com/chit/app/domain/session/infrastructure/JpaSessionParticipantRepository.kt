package com.chit.app.domain.session.infrastructure

import com.chit.app.domain.session.domain.model.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface JpaSessionParticipantRepository : JpaRepository<SessionParticipant, Long> {
    
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
    
}