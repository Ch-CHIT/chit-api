package com.chit.app.global.delegate

import com.chit.app.global.common.response.SuccessResponse
import org.springframework.http.ResponseEntity

typealias EmptyResponse = ResponseEntity<SuccessResponse<Unit>>
typealias MessageResponse = ResponseEntity<SuccessResponse<String>>
