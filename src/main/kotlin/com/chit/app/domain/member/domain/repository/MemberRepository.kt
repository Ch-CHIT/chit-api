package com.chit.app.domain.member.domain.repository

import com.chit.app.domain.member.domain.model.Member
import com.chit.app.domain.member.domain.model.QMember
import com.chit.app.domain.member.infrastructure.MemberJpaRepository
import com.chit.app.global.common.handler.EntitySaveExceptionHandler
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.stereotype.Repository

@Repository
class MemberRepository(
        private val query: JPAQueryFactory,
        private val memberJpaRepository: MemberJpaRepository
) {
    
    private val m: QMember = QMember.member
    
    fun save(member: Member): Member? =
            runCatching { memberJpaRepository.save(member) }
                    .onFailure { EntitySaveExceptionHandler.handle(it) }
                    .getOrThrow()
    
    fun findBy(
            memberId: Long? = null,
            channelName: String? = null,
            channelId: String? = null,
    ): Member? = query
            .selectFrom(m)
            .where(
                memberId?.let { m.id.eq(it) },
                channelId?.let { m.channelId.eq(it) },
                channelName?.let { m.channelName.eq(it) }
            )
            .fetchOne()
    
}