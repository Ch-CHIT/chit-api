package com.chit.app.global.response

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
        val status: Int,
        val error: String? = null,
        val errors: Map<String, String>? = null
) {
    companion object {
        fun internalErrorWithMessage(message: String?): ResponseEntity<ErrorResponse> {
            return failWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, message)
        }
        
        fun failWithMessage(status: HttpStatus, errorMessage: String?): ResponseEntity<ErrorResponse> {
            val errorResponse = ErrorResponse(status.value(), error = errorMessage)
            return ResponseEntity.status(status).body(errorResponse)
        }
        
        fun failWithErrors(status: HttpStatus, errorsMessageList: Map<String, String>?): ResponseEntity<ErrorResponse> {
            val errorResponse = ErrorResponse(status.value(), errors = errorsMessageList)
            return ResponseEntity.status(status).body(errorResponse)
        }
    }
}