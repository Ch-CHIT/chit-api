package com.chit.app.domain.live.infrastructure.response

import com.chit.app.domain.live.domain.model.LiveStatus
import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDateTime

@JsonIgnoreProperties(ignoreUnknown = true)
data class LiveDetailResponse(
        val code: Int?,
        val message: String?,
        val content: Content?
) {
    data class Content(
            val liveId: Long,
            val liveTitle: String,
            val status: LiveStatus,
            val categoryType: String?,
            val liveCategory: String?,
            val liveCategoryValue: String?,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss") val openDate: LocalDateTime,
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss") val closeDate: LocalDateTime?
    )
}