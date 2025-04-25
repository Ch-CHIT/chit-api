package com.chit.app.domain.session.domain.exception

class DuplicateContentsSessionException(
        val errorCode: Int = 43001,
        cause: Throwable? = null
) : RuntimeException("이미 진행 중인 컨텐츠 세션이 존재합니다. 중복 생성을 할 수 없습니다.", cause)

class NoOpenContentsSessionException(
        val errorCode: Int = 43002,
        cause: Throwable? = null
) : RuntimeException("현재 진행 중인 시청자 참여 세션이 없습니다. 다시 확인해 주세요.", cause)

class ParticipantNotFoundException(
        val errorCode: Int = 43003,
        cause: Throwable? = null
) : RuntimeException("해당 세션에 유효한 참여자가 존재하지 않습니다.", cause)

class GameParticipationCodeNotFoundException(
        val errorCode: Int = 43004,
        cause: Throwable? = null
) : RuntimeException("요청하신 게임 참여 코드를 찾을 수 없습니다. 세션 상태를 다시 확인해 주세요.", cause)

class SessionParticipantNotFoundException(
        val errorCode: Int = 43005,
        cause: Throwable? = null
) : RuntimeException("해당 세션 참여 정보를 확인할 수 없습니다. 다시 시도해 주세요.", cause)

class InvalidParticipantException(
        val errorCode: Int = 43006,
        cause: Throwable? = null
) : RuntimeException("유효하지 않은 참여자 정보입니다.", cause)