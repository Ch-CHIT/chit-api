package com.chit.app.domain.auth.presentation.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "OAuth2 로그인 요청")
data class LoginRequestDto(
        
        @Schema(description = "OAuth2 인증 코드 (필수)")
        @field:NotBlank(message = "인증 코드 값이 유효하지 않습니다.")
        val code: String,
        
        @Schema(description = "OAuth2 상태 토큰 (필수)")
        @field:NotBlank(message = "상태 토큰 값이 유효하지 않습니다.")
        val state: String

)