package com.chit.app.domain.member.domain.exception

class MemberRegistrationException(
        val errorCode: Int = 41001,
        cause: Throwable? = null
) : RuntimeException("회원 등록에 실패했습니다.", cause)

class MemberNotFoundException(
        val errorCode: Int = 41002,
        cause: Throwable? = null
) : RuntimeException("회원 정보를 찾을 수 없습니다.", cause)

class MemberValidationException(
        val errorCode: Int = 41003,
        cause: Throwable? = null
) : RuntimeException("제공된 회원 데이터가 유효하지 않습니다. 입력 값을 확인해 주세요.", cause)