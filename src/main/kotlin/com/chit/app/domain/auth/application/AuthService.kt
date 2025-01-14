package com.chit.app.domain.auth.application

import com.chit.app.domain.auth.domain.model.TokenInfo
import com.chit.app.domain.auth.infrastructure.client.ChzzkAuthApiClient
import com.chit.app.domain.auth.infrastructure.security.TokenProvider
import com.chit.app.domain.member.domain.model.Member
import com.chit.app.domain.member.domain.repository.MemberRepository
import com.chit.app.global.delegate.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
        private val tokenService: TokenService,
        private val tokenProvider: TokenProvider,
        private val chzzkAuthApiClient: ChzzkAuthApiClient,
        private val memberRepository: MemberRepository
) {
    
    private val log = logger<AuthService>()
    
    @Transactional
    fun login(code: String, state: String): TokenInfo {
        val chzzkAccessToken = chzzkAuthApiClient.fetchChzzkAccessToken(code, state)
        val channel = chzzkAuthApiClient.fetchChzzkChannelInfo(chzzkAccessToken)
                .let { (channelId, channelName, nickname) -> Channel(channelId, channelName, nickname) }
                .also { log.info("채널 정보 획득: ID={}, 이름={}, 닉네임={}", it.id, it.name, it.nickname) }
        
        return memberRepository.findMemberByChannelName(channel.name)
                ?.createTokenInfo(chzzkAccessToken)
                ?: register(channel, chzzkAccessToken)
    }
    
    fun logout(memberId: Long) {
        tokenService.getRefreshToken(memberId)
                ?.let { token -> tokenService.deleteRefreshToken(token) }
                .also { log.info("사용자 ID {}의 리프레시 토큰이 성공적으로 삭제되었습니다.", memberId) }
    }
    
    private fun register(channel: Channel, chzzkAccessToken: String): TokenInfo {
        return memberRepository.save(
            Member.create(
                channelId = channel.id,
                channelName = channel.name,
                nickname = channel.nickname,
            )
        )?.createTokenInfo(chzzkAccessToken) ?: throw IllegalStateException("회원 등록 과정에서 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.")
    }
    
    private fun Member.createTokenInfo(chzzkAccessToken: String): TokenInfo {
        this.updateLastLoginTime()
        return TokenInfo(
            memberId = id,
            channelId = channelId,
            accessToken = tokenProvider.createAccessToken(id, channelId, chzzkAccessToken),
            refreshToken = tokenProvider.createRefreshToken(id),
        ).also { log.info("토큰 생성 완료 - 사용자 ID: {}, 채널 ID: {}, 채널 이름: {}", this.id, channelId, this.channelName) }
    }
    
    private data class Channel(
            val id: String,
            val name: String,
            val nickname: String
    )
    
}