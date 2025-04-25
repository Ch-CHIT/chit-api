package com.chit.app.domain.auth.domain.exception

class TokenReissueException(
        val errorCode: Int = 40001,
        cause: Throwable? = null
) : RuntimeException("토큰 재발급 중 오류가 발생했습니다. 관리자에게 문의해 주세요.", cause)

class MissingTokenException(
        val errorCode: Int = 40002,
        cause: Throwable? = null
) : RuntimeException("필수 토큰이 누락되었습니다. 요청 헤더를 확인해 주세요.", cause)

class InvalidTokenException(
        val errorCode: Int = 40003,
        cause: Throwable? = null
) : RuntimeException("유효하지 않은 토큰입니다. 다시 로그인해 주세요.", cause)

class ExpiredTokenException(
        val errorCode: Int = 40004,
        cause: Throwable? = null
) : RuntimeException("토큰이 만료되었습니다. 다시 로그인해 주세요.", cause)

class UnsupportedTokenException(
        val errorCode: Int = 40005,
        cause: Throwable? = null
) : RuntimeException("지원되지 않는 토큰 형식입니다.", cause)

class MalformedTokenException(
        val errorCode: Int = 40006,
        cause: Throwable? = null
) : RuntimeException("토큰 형식이 올바르지 않습니다.", cause)

class InvalidSignatureException(
        val errorCode: Int = 40007,
        cause: Throwable? = null
) : RuntimeException("토큰 서명이 유효하지 않습니다.", cause)