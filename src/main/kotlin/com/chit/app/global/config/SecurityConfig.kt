package com.chit.app.global.config

import com.chit.app.domain.auth.infrastructure.security.JwtAuthenticationEntryPoint
import com.chit.app.domain.auth.infrastructure.security.TokenProvider
import com.chit.app.domain.auth.presentation.filter.JwtAuthenticationFilter
import com.chit.app.domain.auth.infrastructure.properties.JwtFilterProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
        private val tokenProvider: TokenProvider,
        private val properties: JwtFilterProperties,
        private val jwtAuthenticationEntryPoint: JwtAuthenticationEntryPoint,
        
        @Value("\${cors.url}") private val corsUrl: String
) {
    
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .headers { it.frameOptions { frame -> frame.sameOrigin() } }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { it.authenticationEntryPoint(jwtAuthenticationEntryPoint) }
            .requestCache { it.disable() }
            .securityContext { it.requireExplicitSave(false) }
            .authorizeHttpRequests { request ->
                request
                        .requestMatchers(
                            "/static/**",
                            "/public/**",
                            "/h2-console/**",
                            "/swagger-ui/**",
                            "/v3/api-docs/**"
                        ).permitAll()
                        .requestMatchers("/", "/api/v1/auth/**").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter::class.java)
            .build()
    
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", CorsConfiguration().apply {
                allowedOrigins = listOf(this@SecurityConfig.corsUrl)
                allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
                allowedHeaders = listOf("X-Requested-With", "Content-Type", "Authorization", "X-XSRF-token")
                allowCredentials = true
                maxAge = 3600L
            })
        }
    }
    
    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()
    
    @Bean
    fun jwtAuthenticationFilter(): JwtAuthenticationFilter = JwtAuthenticationFilter(tokenProvider, properties)
    
}