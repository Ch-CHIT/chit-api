package com.chit.app.domain.auth.application

import com.chit.app.domain.auth.domain.exception.InvalidTokenException
import com.chit.app.domain.auth.domain.exception.MissingTokenException
import com.chit.app.domain.auth.domain.exception.TokenReissueException
import com.chit.app.domain.auth.domain.model.TokenInfo
import com.chit.app.domain.auth.infrastructure.client.ChzzkAuthApiClient
import com.chit.app.domain.auth.infrastructure.security.TokenProvider
import com.chit.app.domain.member.domain.exception.MemberNotFoundException
import com.chit.app.domain.member.domain.exception.MemberRegistrationException
import com.chit.app.domain.member.domain.model.Member
import com.chit.app.domain.member.domain.repository.MemberRepository
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
        private val tokenProvider: TokenProvider,
        private val memberRepository: MemberRepository,
        private val chzzkAuthApiClient: ChzzkAuthApiClient,
) {
    
    private val log = logger<AuthService>()
    
    @Transactional
    fun login(code: String, state: String): TokenInfo {
        val chzzkAccessToken = chzzkAuthApiClient.fetchChzzkAccessToken(code, state)
        val channel = chzzkAuthApiClient.fetchChzzkChannelInfo(chzzkAccessToken)
                .let { (channelId, channelName, nickname) -> Channel(channelId, channelName, nickname) }
        
        return memberRepository.findBy(channelId = channel.id)
                ?.createTokenInfo(chzzkAccessToken)
                ?: register(channel, chzzkAccessToken)
    }
    
    private fun register(channel: Channel, chzzkAccessToken: String): TokenInfo =
            memberRepository.save(Member.create(channel.id, channel.name, channel.nickname))
                    ?.createTokenInfo(chzzkAccessToken)
                    ?: throw MemberRegistrationException()
    
    @Transactional(readOnly = true)
    fun refreshAccessToken(requestRefreshToken: String?): TokenInfo {
        if (requestRefreshToken.isNullOrBlank()) {
            throw MissingTokenException()
        }
        
        val memberId = try {
            tokenProvider.getMemberIdFromToken(requestRefreshToken)
        } catch (e: Exception) {
            log.warn("토큰에서 memberId 추출 실패: ${e.message}")
            throw InvalidTokenException(cause = e)
        }
        
        val member = memberRepository.findBy(memberId = memberId) ?: throw MemberNotFoundException()
        log.info { "회원 인증 성공. memberId: $memberId, channel: ${member.channelName}" }
        
        return try {
            val newAccessToken = tokenProvider.createAccessToken(member.id, member.channelId, member.channelName)
            val newRefreshToken = tokenProvider.createRefreshToken(member.id)
            TokenInfo(accessToken = newAccessToken, refreshToken = newRefreshToken)
                    .also { log.info { "새로운 토큰 발급 완료. memberId: $memberId" } }
        } catch (e: Exception) {
            log.error("토큰 발급 중 오류 발생: ${e.message}", e)
            throw TokenReissueException()
        }
    }
    
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