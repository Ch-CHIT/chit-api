package com.chit.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing

@EnableJpaAuditing
@SpringBootApplication
class ChitAppApplication

fun main(args: Array<String>) {
    runApplication<ChitAppApplication>(*args)
}