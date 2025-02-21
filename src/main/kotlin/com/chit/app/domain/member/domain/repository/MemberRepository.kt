package com.chit.app.domain.member.domain.repository

import com.chit.app.domain.member.domain.model.Member
import com.chit.app.domain.member.infrastructure.MemberJpaRepository
import com.chit.app.global.common.handler.EntitySaveExceptionHandler
import org.springframework.stereotype.Repository

@Repository
class MemberRepository(
        private val memberJpaRepository: MemberJpaRepository
) {
    
    fun save(member: Member): Member? =
            runCatching { memberJpaRepository.save(member) }
                    .onFailure { EntitySaveExceptionHandler.handle(it) }
                    .getOrThrow()
    
    fun findMemberByChannelName(channelName: String): Member? =
            memberJpaRepository.findByChannelName(channelName)
    
    fun fetchMemberByChannelId(channelId: String): Member? =
            memberJpaRepository.findByChannelId(channelId)
    
}