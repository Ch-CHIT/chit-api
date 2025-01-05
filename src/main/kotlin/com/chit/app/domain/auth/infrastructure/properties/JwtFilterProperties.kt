package com.chit.app.domain.auth.infrastructure.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "jwt.filter")
data class JwtFilterProperties(
        val whitelistUrls: List<String>,
        val sseUrlPatterns: List<String>
)