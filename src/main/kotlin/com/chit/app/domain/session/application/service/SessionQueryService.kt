package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.global.common.response.SuccessResponse.PagedResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SessionQueryService(
        private val sessionRepository: SessionRepository,
) {
    
    fun getContentsSessionWithParticipants(streamerId: Long, pageable: Pageable): ContentsSessionResponseDto {
        val contentsSession = getOpenContentsSession(streamerId)
        val participants = sessionRepository.findPagedParticipantsBySessionCode(contentsSession.sessionCode, pageable)
        return ContentsSessionResponseDto(
            sessionCode = contentsSession.sessionCode,
            gameParticipationCode = contentsSession.gameParticipationCode,
            maxGroupParticipants = contentsSession.maxGroupParticipants,
            currentParticipants = contentsSession.currentParticipants,
            participants = PagedResponse.from(participants)
        )
    }
    
    fun getGameParticipationCode(sessionCode: String, viewerId: Long): ContentsSessionResponseDto {
        val gameParticipationCode = sessionRepository.findGameParticipationCodeBy(sessionCode, viewerId)
                ?: throw IllegalArgumentException("해당 세션에 참가자 정보가 존재하지 않습니다. 다시 확인해 주세요.")
        return ContentsSessionResponseDto(gameParticipationCode = gameParticipationCode)
    }
    
    fun getOpenContentsSession(streamerId: Long): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(streamerId = streamerId)
                ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다. 다시 확인해 주세요.")
    }
    
    fun getParticipant(viewerId: Long, sessionId: Long): SessionParticipant {
        return sessionRepository.findParticipantBy(viewerId, sessionId = sessionId)
                ?: throw IllegalArgumentException("해당 세션에 유효한 참여자가 존재하지 않습니다.")
    }
    
}