package com.chit.app.global.util

import com.aventrix.jnanoid.jnanoid.NanoIdUtils

object NanoIdUtil {
    private const val DEFAULT_SIZE = 12
    private val DEFAULT_ALPHABET = ('0'..'9').toList() + ('A'..'Z').toList() + ('a'..'z').toList()
    
    fun generate(): String = NanoIdUtils.randomNanoId(
        NanoIdUtils.DEFAULT_NUMBER_GENERATOR,
        DEFAULT_ALPHABET.toCharArray(),
        DEFAULT_SIZE
    )
}