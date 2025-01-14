package com.chit.app.global.delegate

import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.global.response.SuccessResponse
import org.springframework.http.ResponseEntity

typealias Void = ResponseEntity<SuccessResponse<Unit>>
typealias Message = ResponseEntity<SuccessResponse<String>>
typealias NewContentsSession = ResponseEntity<SuccessResponse<ContentsSessionResponseDto?>>
typealias DetailContentsSession = ResponseEntity<SuccessResponse<ContentsSessionResponseDto?>>