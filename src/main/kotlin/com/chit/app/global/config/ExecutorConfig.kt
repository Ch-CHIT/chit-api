package com.chit.app.global.config

import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Configuration
class ExecutorConfig {
    
    private val virtualThreadExecutor: ExecutorService = Executors.newVirtualThreadPerTaskExecutor()
    
    @Bean(name = ["taskExecutor"])
    fun taskExecutor(): ExecutorService {
        return virtualThreadExecutor
    }
    
    @PreDestroy
    fun shutdownExecutor() {
        if (!virtualThreadExecutor.isShutdown) {
            virtualThreadExecutor.shutdown()
        }
    }
    
}