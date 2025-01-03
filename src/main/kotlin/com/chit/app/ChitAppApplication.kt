package com.chit.app

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ChitAppApplication

fun main(args: Array<String>) {
    runApplication<ChitAppApplication>(*args)
}