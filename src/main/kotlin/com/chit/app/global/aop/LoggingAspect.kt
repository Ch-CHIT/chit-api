package com.chit.app.global.aop

import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Aspect
@Component
class LoggingAspect {
    
    private val log = LoggerFactory.getLogger(this::class.java)
    
    @Around("@annotation(com.chit.app.global.common.annotation.LogExecutionTime)")
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val start = System.currentTimeMillis()
        val result = joinPoint.proceed()
        val executionTime = System.currentTimeMillis() - start
        log.debug("{} executed in {} ms", joinPoint.signature, executionTime)
        return result
    }
}