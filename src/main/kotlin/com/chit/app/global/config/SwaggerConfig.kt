package com.chit.app.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    
    companion object {
        private const val AUTH_SCHEME = "Authorization"
        private const val SCHEME = "bearer"
        private const val FORMAT = "JWT"
        private const val TITLE = "CHIT API"
        private const val VERSION = "1.0.0"
    }
    
    @Bean
    fun customOpenAPI(): OpenAPI = OpenAPI().apply {
        components = Components()
                .addSecuritySchemes(
                    AUTH_SCHEME, SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme(SCHEME)
                            .bearerFormat(FORMAT)
                            .name(AUTH_SCHEME)
                )
        info = Info()
                .title(TITLE)
                .version(VERSION)
                .description("[치지직 로그인 바로가기](http://localhost:8080)")
        
        addSecurityItem(SecurityRequirement().addList(AUTH_SCHEME))
    }
}