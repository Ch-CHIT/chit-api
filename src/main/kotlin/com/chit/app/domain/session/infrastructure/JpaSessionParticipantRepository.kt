package com.chit.app.domain.session.infrastructure

import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface JpaSessionParticipantRepository : JpaRepository<SessionParticipant, Long> {
    
    @Modifying
    @Transactional
    @Query(
        """
        UPDATE SessionParticipant sp
        SET sp._status = 'LEFT'
        WHERE sp._status != 'LEFT'
    """
    )
    fun closeAllNonLeftParticipants(): Int
    
}