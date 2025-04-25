package com.chit.app.domain.live.domain.exception

class InvalidLiveApiRequestException(
        val errorCode: Int = 42001,
        cause: Throwable? = null
) : RuntimeException("잘못된 API 요청 경로입니다. 관리자에게 문의해 주세요.", cause)

class LiveFetchException(
        val errorCode: Int = 42002,
        cause: Throwable? = null
) : RuntimeException("라이브 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.", cause)

class LiveNotFoundException(
        val errorCode: Int = 42003,
        cause: Throwable? = null,
) : RuntimeException("요청하신 라이브 정보를 찾을 수 없습니다. 방송이 종료되었거나 해당 채널이 존재하지 않습니다.", cause)