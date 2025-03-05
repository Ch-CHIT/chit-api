package com.chit.app.domain.member.application

import com.chit.app.domain.member.domain.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MemberService(
        private val memberRepository: MemberRepository
) {
    
    @Transactional(readOnly = true)
    fun getChzzkNickname(memberId: Long): String =
            memberRepository.findBy(memberId = memberId)?.channelName ?: throw IllegalArgumentException("참여자 정보를 찾을 수 없습니다. 다시 시도해 주세요.")
    
}