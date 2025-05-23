package com.chit.app.global.common.response

import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorResponse(
        val status: Int,
        val code: Int? = null,
        val message: String? = null,
        val errors: Map<String, String>? = null
) {
    companion object {
        fun internalErrorWithMessage(message: String?): ResponseEntity<ErrorResponse> {
            return failWithMessage(HttpStatus.INTERNAL_SERVER_ERROR, message = message, code = 50000)
        }
        
        fun failWithMessage(status: HttpStatus, message: String?, code: Int = 0): ResponseEntity<ErrorResponse> {
            val errorResponse = ErrorResponse(status.value(), message = message, code = code)
            return ResponseEntity.status(status).body(errorResponse)
        }
        
        fun failWithErrors(status: HttpStatus, errorsMessageList: Map<String, String>?, code: Int = 0): ResponseEntity<ErrorResponse> {
            val errorResponse = ErrorResponse(status.value(), errors = errorsMessageList, code = code)
            return ResponseEntity.status(status).body(errorResponse)
        }
    }
}