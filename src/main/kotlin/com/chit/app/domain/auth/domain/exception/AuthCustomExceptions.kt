package com.chit.app.domain.auth.domain.exception

class TokenReissueException(
        val errorCode: Int = 40001,
        cause: Throwable? = null
) : RuntimeException("토큰 재발급 중 오류가 발생했습니다. 관리자에게 문의해 주세요.", cause)

class MissingTokenException(
        val errorCode: Int = 40002,
        cause: Throwable? = null
) : RuntimeException("필수 토큰이 누락되었습니다. 요청 헤더를 확인해 주세요.", cause)

class InvalidAuthCodeStateException(
        val errorCode: Int = 40003,
        cause: Throwable? = null
) : RuntimeException("인증 코드 또는 상태 값이 올바르지 않습니다. 다시 시도해 주세요.", cause)

class AuthTokenRequestException(
        val errorCode: Int = 40004,
        cause: Throwable? = null
) : RuntimeException("액세스 토큰 발급 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.", cause)

class AuthCodeForbiddenException(
        val errorCode: Int = 40005,
        cause: Throwable? = null
) : RuntimeException("잘못된 인증 코드입니다. 다시 시도해 주세요.", cause)

class InvalidTokenException(
        val errorCode: Int = 40006,
        cause: Throwable? = null
) : RuntimeException("유효하지 않은 토큰입니다. 다시 로그인해 주세요.", cause)

class ExpiredTokenException(
        val errorCode: Int = 40007,
        cause: Throwable? = null
) : RuntimeException("토큰이 만료되었습니다. 다시 로그인해 주세요.", cause)

class UnsupportedTokenException(
        val errorCode: Int = 40008,
        cause: Throwable? = null
) : RuntimeException("지원되지 않는 토큰 형식입니다.", cause)

class MalformedTokenException(
        val errorCode: Int = 40009,
        cause: Throwable? = null
) : RuntimeException("토큰 형식이 올바르지 않습니다.", cause)

class InvalidSignatureException(
        val errorCode: Int = 40010,
        cause: Throwable? = null
) : RuntimeException("토큰 서명이 유효하지 않습니다.", cause)

class AuthUnauthorizedException(
        val errorCode: Int = 40011,
        cause: Throwable? = null
) : RuntimeException("잘못된 인증 정보입니다. 다시 로그인해 주세요.", cause)

class AuthApiPathNotFoundException(
        val errorCode: Int = 40012,
        cause: Throwable? = null
) : RuntimeException("채널 정보 API 경로를 확인해 주세요.", cause)

class AuthAccessDeniedException(
        val errorCode: Int = 40013,
        cause: Throwable? = null
) : RuntimeException("접근 권한이 없습니다. 요청 권한을 확인해 주세요.", cause)

class AuthChannelFetchException(
        val errorCode: Int = 40014,
        cause: Throwable? = null
) : RuntimeException("채널 정보 조회 중 문제가 발생했습니다. 잠시 후 다시 시도해 주세요.", cause)

class InvalidChannelInfoException(
        val errorCode: Int = 40015,
        cause: Throwable? = null
) : RuntimeException("채널 정보를 가져오는 데 실패했습니다. 유효하지 않은 토큰이거나 요청 경로가 잘못되었습니다.", cause)

class AuthenticatedUserNotFoundException(
        val errorCode: Int = 40016,
        cause: Throwable? = null
) : RuntimeException("인증된 사용자 정보가 존재하지 않습니다.", cause)
