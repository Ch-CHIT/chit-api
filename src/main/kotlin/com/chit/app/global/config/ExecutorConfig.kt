package com.chit.app.global.config

import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class ExecutorConfig {
    
    private val executor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
    
    @Bean
    fun virtualThreadExecutor(): ExecutorService {
        return executor
    }
    
    @PreDestroy
    fun shutdownExecutor() {
        if (!executor.isShutdown) {
            executor.shutdown()
        }
    }
    
}