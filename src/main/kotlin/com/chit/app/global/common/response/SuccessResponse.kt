package com.chit.app.global.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class SuccessResponse<T>(
        val status: Int,
        val code: Int = 20000,
        val data: T? = null
) {
    companion object {
        fun <Unit> success(): ResponseEntity<SuccessResponse<Unit>> =
                ResponseEntity.ok(
                    SuccessResponse(
                        status = HttpStatus.OK.value(),
                    )
                )
        
        fun <T> successWithData(data: T): ResponseEntity<SuccessResponse<T>> =
                ResponseEntity.ok(
                    SuccessResponse(
                        status = HttpStatus.OK.value(),
                        data = data
                    )
                )
        
        fun <T> successWithPagedData(page: Page<T>): ResponseEntity<SuccessResponse<PagedResponse<T>>> =
                ResponseEntity.ok(
                    SuccessResponse(
                        status = HttpStatus.OK.value(),
                        data = PagedResponse.from(page)
                    )
                )
    }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    data class PagedResponse<T>(
            val content: List<T>,
            val page: Int,
            val size: Int,
            val totalElements: Long,
            val totalPages: Int,
            val hasNext: Boolean,
            val hasPrevious: Boolean
    ) {
        companion object {
            fun <T> from(page: Page<T>): PagedResponse<T> =
                    PagedResponse(
                        content = page.content,
                        page = page.number,
                        size = page.size,
                        totalElements = page.totalElements,
                        totalPages = page.totalPages,
                        hasNext = page.hasNext(),
                        hasPrevious = page.hasPrevious()
                    )
        }
    }
}