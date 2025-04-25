package com.chit.app.global.delegate

import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.global.response.SuccessResponse
import org.springframework.http.ResponseEntity

typealias EmptyResponse = ResponseEntity<SuccessResponse<Unit>>
typealias MessageResponse = ResponseEntity<SuccessResponse<String>>
typealias NewContentsSessionResponse = ResponseEntity<SuccessResponse<ContentsSessionResponseDto?>>
typealias DetailContentsSessionResponse = ResponseEntity<SuccessResponse<ContentsSessionResponseDto?>>
typealias GameCodeResponse = ResponseEntity<SuccessResponse<ContentsSessionResponseDto?>>