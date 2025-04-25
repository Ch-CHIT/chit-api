package com.chit.app.domain.member.application

import com.chit.app.domain.member.domain.exception.MemberNotFoundException
import com.chit.app.domain.member.domain.model.Member
import com.chit.app.domain.member.domain.repository.MemberRepository
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberQueryService(
        private val memberRepository: MemberRepository
) {
    
    private val log = logger<MemberQueryService>()
    
    @Transactional(readOnly = true)
    fun getMember(memberId: Long): Member =
            memberRepository.findBy(memberId = memberId) ?: run {
                log.error("회원 ID $memberId 에 해당하는 회원을 찾을 수 없습니다.")
                throw MemberNotFoundException()
            }
}