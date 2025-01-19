package com.chit.app.domain.session.presentation.dto

import jakarta.validation.constraints.Min

data class ContentsSessionUpsertRequestDto(
        
        val gameParticipationCode: String?,
        
        @field:Min(value = 1, message = "최소 1명 이상의 참가자가 필요합니다.")
        val maxGroupParticipants: Int,
)