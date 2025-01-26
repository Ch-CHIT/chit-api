package com.chit.app.domain.live.infrastructure

import com.chit.app.domain.live.domain.model.LiveStream
import org.springframework.data.jpa.repository.JpaRepository

interface JpaLiveStreamRepository : JpaRepository<LiveStream, Long>