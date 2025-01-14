package com.chit.app.domain.member.infrastructure

import com.chit.app.domain.member.domain.model.Member
import org.springframework.data.jpa.repository.JpaRepository

interface MemberJpaRepository : JpaRepository<Member, Long> {
    fun findByChannelId(channelId: String): Member?
    fun findByChannelName(channelName: String): Member?
}