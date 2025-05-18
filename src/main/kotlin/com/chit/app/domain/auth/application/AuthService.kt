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
        log.info("[요청] 로그인 요청 수신 (code={}, state={})", code, state)
        val chzzkAccessToken = chzzkAuthApiClient.fetchChzzkAccessToken(code, state)
        val channel = chzzkAuthApiClient.fetchChzzkChannelInfo(chzzkAccessToken)
                .let { (channelId, channelName, nickname) -> Channel(channelId, channelName, nickname) }
        return memberRepository.findBy(channelId = channel.id)
                ?.also { log.info("[성공] 기존 회원 로그인 완료 (memberId={}, channelId={})", it.id, it.channelId) }
                ?.createTokenInfo(chzzkAccessToken)
                ?: run {
                    log.info("[진행] 신규 회원 등록 진행 (channelId={})", channel.id)
                    register(channel, chzzkAccessToken)
                }
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
            log.warn("[실패] 리프레시 토큰에서 회원 ID 추출 실패 (refreshToken=****) [부가정보: ${e.message}]")
            throw InvalidTokenException(cause = e)
        }
        
        val member = memberRepository.findBy(memberId = memberId) ?: throw MemberNotFoundException()
        log.info("[성공] 리프레시 토큰 인증 성공 (memberId=$memberId, channelName=${member.channelName})")
        
        return try {
            val newAccessToken = tokenProvider.createAccessToken(member.id, member.channelId, member.channelName)
            val newRefreshToken = tokenProvider.createRefreshToken(member.id)
            log.info("[성공] 액세스/리프레시 토큰 재발급 완료 (memberId=$memberId)")
            TokenInfo(accessToken = newAccessToken, refreshToken = newRefreshToken)
        } catch (e: Exception) {
            log.error("[실패] 토큰 재발급 중 예외 발생 (memberId=$memberId) [부가정보: ${e.message}]", e)
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