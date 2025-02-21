package com.chit.app.global.common.logging

import mu.KotlinLogging

inline fun <reified T : Any> logger() = KotlinLogging.logger { T::class.java }