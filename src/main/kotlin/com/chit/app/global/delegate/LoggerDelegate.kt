package com.chit.app.global.delegate

import mu.KotlinLogging

inline fun <reified T : Any> logger() = KotlinLogging.logger { T::class.java }