package com.chit.app.domain.auth.application

import com.chit.app.domain.auth.domain.model.TokenInfo
import com.chit.app.domain.auth.infrastructure.client.ChzzkAuthApiClient
import com.chit.app.domain.auth.infrastructure.security.TokenProvider
import com.chit.app.domain.member.domain.model.Member
import com.chit.app.domain.member.domain.repository.MemberRepository
import com.chit.app.global.common.logging.logger
import io.jsonwebtoken.JwtException
import jakarta.persistence.EntityNotFoundException
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
                    ?: throw IllegalStateException("회원 등록 과정에서 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.")
    
    @Transactional(readOnly = true)
    fun refreshAccessToken(requestRefreshToken: String?): TokenInfo {
        if (requestRefreshToken.isNullOrBlank()) {
            throw JwtException("액세스 토큰 또는 리프레시 토큰이 누락되었습니다.")
        }
        
        val memberId = try {
            tokenProvider.getMemberIdFromToken(requestRefreshToken)
        } catch (e: Exception) {
            log.warn("토큰에서 memberId 추출 실패: ${e.message}")
            throw JwtException("유효하지 않은 액세스 토큰입니다.", e)
        }
        
        val member = memberRepository.findBy(memberId = memberId)
                ?: throw EntityNotFoundException("ID가 $memberId 인 회원을 찾을 수 없습니다.")
        log.info { "회원 인증 성공. memberId: $memberId, channel: ${member.channelName}" }
        
        return try {
            val newAccessToken = tokenProvider.createAccessToken(member.id, member.channelId, member.channelName)
            val newRefreshToken = tokenProvider.createRefreshToken(member.id)
            log.info { "새로운 토큰 발급 완료. memberId: $memberId" }
            
            TokenInfo(accessToken = newAccessToken, refreshToken = newRefreshToken)
        } catch (e: Exception) {
            log.error("토큰 발급 중 오류 발생: ${e.message}", e)
            throw IllegalStateException("토큰 재발급에 실패했습니다. 다시 시도해 주세요.")
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