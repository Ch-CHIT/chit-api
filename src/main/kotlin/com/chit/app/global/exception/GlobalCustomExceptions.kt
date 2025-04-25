package com.chit.app.global.exception

class DataIntegrityException(
        val errorCode: Int = 49001,
        cause: Throwable? = null
) : RuntimeException("저장에 실패했습니다. 이미 존재하는 정보이거나 필수 값이 누락되었습니다.", cause)

class OptimisticLockingConflictException(
        val errorCode: Int = 49002,
        cause: Throwable? = null
) : RuntimeException("다른 작업이 동시에 처리되어 저장에 실패했습니다.", cause)

class TransactionSystemFailureException(
        val errorCode: Int = 49003,
        cause: Throwable? = null
) : RuntimeException("시스템 문제로 저장에 실패했습니다.", cause)

class GlobalPersistenceException(
        val errorCode: Int = 49004,
        cause: Throwable? = null
) : RuntimeException("예기치 못한 오류가 발생했습니다.", cause)
