package com.chit.app.domain.auth.application

import com.chit.app.domain.auth.domain.model.TokenInfo
import com.chit.app.domain.auth.infrastructure.client.ChzzkAuthApiClient
import com.chit.app.domain.auth.infrastructure.security.TokenProvider
import com.chit.app.domain.member.domain.model.Member
import com.chit.app.domain.member.domain.repository.MemberRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
        private val tokenService: TokenService,
        private val tokenProvider: TokenProvider,
        private val chzzkAuthApiClient: ChzzkAuthApiClient,
        private val memberRepository: MemberRepository
) {
    
    @Transactional
    fun login(code: String, state: String): TokenInfo {
        val chzzkAccessToken = chzzkAuthApiClient.fetchChzzkAccessToken(code, state)
        val channel = chzzkAuthApiClient.fetchChzzkChannelInfo(chzzkAccessToken)
                .let { (channelId, channelName, nickname) -> Channel(channelId, channelName, nickname) }
        
        return memberRepository.findMemberByChannelName(channel.name)
                ?.createTokenInfo(chzzkAccessToken)
                ?: register(channel, chzzkAccessToken)
    }
    
    fun logout(memberId: Long) = tokenService.deleteRefreshTokenByMemberId(memberId)
    
    private fun register(channel: Channel, chzzkAccessToken: String): TokenInfo =
            memberRepository.save(Member.create(channel.id, channel.name, channel.nickname))
                    ?.createTokenInfo(chzzkAccessToken)
                    ?: throw IllegalStateException("회원 등록 과정에서 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.")
    
    private fun Member.createTokenInfo(chzzkAccessToken: String): TokenInfo =
            this.apply { updateLastLoginTime() }
                    .let {
                        TokenInfo(
                            memberId = id,
                            channelId = channelId,
                            accessToken = tokenProvider.createAccessToken(id, channelId, chzzkAccessToken),
                            refreshToken = tokenProvider.createRefreshToken(id)
                        )
                    }
    
    private data class Channel(
            val id: String,
            val name: String,
            val nickname: String
    )
    
}