package com.chit.app

import com.chit.app.domain.auth.infrastructure.properties.JwtFilterProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableConfigurationProperties(JwtFilterProperties::class)
@EnableScheduling
@EnableJpaAuditing
@EnableCaching
@EnableAsync
@SpringBootApplication
class ChitAppApplication

fun main(args: Array<String>) {
    runApplication<ChitAppApplication>(*args)
}