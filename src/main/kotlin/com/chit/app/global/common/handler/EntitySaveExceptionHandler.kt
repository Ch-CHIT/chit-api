package com.chit.app.global.common.handler

import com.chit.app.global.common.logging.logger
import com.chit.app.global.exception.DataIntegrityException
import com.chit.app.global.exception.GlobalPersistenceException
import com.chit.app.global.exception.OptimisticLockingConflictException
import com.chit.app.global.exception.TransactionSystemFailureException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.dao.OptimisticLockingFailureException
import org.springframework.transaction.TransactionSystemException

object EntitySaveExceptionHandler {
    private val log = logger<EntitySaveExceptionHandler>()
    
    fun handle(e: Throwable): Nothing {
        when (e) {
            is DataIntegrityViolationException   -> {
                log.error("데이터 무결성 제약 조건 위반으로 저장 실패: {}", e.message)
                throw DataIntegrityException(cause = e)
            }
            
            is OptimisticLockingFailureException -> {
                log.error("동시 작업 충돌로 저장 실패: {}", e.message)
                throw OptimisticLockingConflictException(cause = e)
            }
            
            is TransactionSystemException        -> {
                log.error("트랜잭션 처리 중 시스템 오류 발생: {}", e.message)
                throw TransactionSystemFailureException(cause = e)
            }
            
            else                                 -> {
                log.error("알 수 없는 이유로 저장 실패: {}", e.message)
                throw GlobalPersistenceException(cause = e)
            }
        }
    }
}