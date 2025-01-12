package com.chit.app.domain.member.domain.repository

import com.chit.app.domain.member.domain.model.Member
import com.chit.app.domain.member.infrastructure.MemberJpaRepository
import org.springframework.stereotype.Repository

@Repository
class MemberRepository(
        private val memberJpaRepository: MemberJpaRepository
) {
    
    fun save(member: Member): Member = memberJpaRepository.save(member)
    
    fun fetchMemberByChannelId(channelId: String): Member? = memberJpaRepository.findByChannelId(channelId)
    
}