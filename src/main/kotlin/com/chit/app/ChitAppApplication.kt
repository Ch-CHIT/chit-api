package com.chit.app

import com.chit.app.domain.auth.infrastructure.properties.JwtFilterProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableConfigurationProperties(JwtFilterProperties::class)
@EnableJpaAuditing
@SpringBootApplication
class ChitAppApplication

fun main(args: Array<String>) {
    runApplication<ChitAppApplication>(*args)
}