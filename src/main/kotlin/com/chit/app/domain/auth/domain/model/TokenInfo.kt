package com.chit.app.domain.auth.domain.model

data class TokenInfo(
        val accessToken: String,
        val refreshToken: String
)