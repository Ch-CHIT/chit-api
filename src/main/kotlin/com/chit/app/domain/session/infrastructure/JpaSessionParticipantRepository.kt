package com.chit.app.domain.session.infrastructure

import com.chit.app.domain.session.domain.model.SessionParticipant
import org.springframework.data.jpa.repository.JpaRepository

interface JpaSessionParticipantRepository : JpaRepository<SessionParticipant, Long>