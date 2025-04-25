package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.domain.exception.GameParticipationCodeNotFoundException
import com.chit.app.domain.session.domain.exception.NoOpenContentsSessionException
import com.chit.app.domain.session.domain.exception.ParticipantNotFoundException
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.global.response.SuccessResponse.PagedResponse
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
        val gameParticipationCode = sessionRepository.findGameParticipationCodeBy(sessionCode, viewerId) ?: throw GameParticipationCodeNotFoundException()
        return ContentsSessionResponseDto(gameParticipationCode = gameParticipationCode)
    }
    
    fun getOpenContentsSession(streamerId: Long): ContentsSession =
            sessionRepository.findOpenContentsSessionBy(streamerId = streamerId) ?: throw NoOpenContentsSessionException()
    
    fun getParticipant(viewerId: Long, sessionId: Long): SessionParticipant =
            sessionRepository.findParticipantBy(viewerId, sessionId = sessionId) ?: throw ParticipantNotFoundException()
    
}