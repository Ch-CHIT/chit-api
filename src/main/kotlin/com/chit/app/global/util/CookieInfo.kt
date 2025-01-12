package com.chit.app.global.util

enum class CookieInfo(
        val maxAge: Int
) {
    REFRESH_TOKEN(60 * 60 * 24)
}