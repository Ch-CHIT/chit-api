package com.chit.app.domain.auth.application

import com.chit.app.domain.auth.domain.model.RefreshToken
import com.chit.app.global.common.logging.logger
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.CachePut
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Service

@Service
class TokenService {
    
    private val log = logger<TokenService>()
    
    @Cacheable(value = ["refreshTokenCache"], key = "#memberId")
    fun getRefreshToken(memberId: Long): RefreshToken? {
        return null
    }
    
    @CachePut(value = ["refreshTokenCache"], key = "#refreshToken.memberId")
    fun putRefreshToken(refreshToken: RefreshToken): RefreshToken {
        return refreshToken
    }
    
    @CacheEvict(value = ["refreshTokenCache"], key = "#refreshToken.memberId")
    fun deleteRefreshToken(refreshToken: RefreshToken) {
        log.info("회원 ID: ${refreshToken.memberId} 에 대한 리프레시 토큰이 캐시에서 삭제되었습니다.")
    }
    
}