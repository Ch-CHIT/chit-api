package com.chit.app.domain.session.infrastructure

import com.chit.app.domain.session.domain.model.ContentsSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional

interface JpaContentsSessionRepository : JpaRepository<ContentsSession, Long> {
    
    @Modifying
    @Transactional
    @Query("""
        UPDATE ContentsSession cs
        SET cs._status = 'CLOSE'
        WHERE cs._status = 'OPEN'
    """)
    fun closeAllOpenSessions(): Int
    
}