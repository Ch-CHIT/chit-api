package com.chit.app.global.handler

import com.chit.app.global.delegate.logger
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.TransactionSystemException

object EntitySaveExceptionHandler {
    private val log = logger<EntitySaveExceptionHandler>()
    
    fun handle(exception: Throwable): Nothing {
        when (exception) {
            is DataIntegrityViolationException   -> {
                log.error("데이터 무결성 제약 조건 위반으로 저장 실패: {}", exception.message)
                throw IllegalArgumentException("저장에 실패했습니다. 이미 존재하는 정보이거나 필수 값이 누락되었습니다.", exception)
            }
            
            is OptimisticLockingFailureException -> {
                log.error("동시 작업 충돌로 저장 실패: {}", exception.message)
                throw IllegalStateException("다른 작업이 동시에 처리되어 저장에 실패했습니다.", exception)
            }
            
            is TransactionSystemException        -> {
                log.error("트랜잭션 처리 중 시스템 오류 발생: {}", exception.message)
                throw IllegalStateException("시스템 문제로 저장에 실패했습니다.", exception)
            }
            
            else                                 -> {
                log.error("알 수 없는 이유로 저장 실패: {}", exception.message)
                throw IllegalStateException("예기치 못한 오류가 발생했습니다.", exception)
            }
        }
    }
}