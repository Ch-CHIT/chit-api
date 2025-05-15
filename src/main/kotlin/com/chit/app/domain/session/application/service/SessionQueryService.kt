package com.chit.app.domain.session.application.service

import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.domain.exception.GameParticipationCodeNotFoundException
import com.chit.app.domain.session.domain.exception.NoOpenContentsSessionException
import com.chit.app.domain.session.domain.exception.ParticipantNotFoundException
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.global.common.logging.logger
import com.chit.app.global.common.response.SuccessResponse.PagedResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class SessionQueryService(
        private val sessionRepository: SessionRepository,
) {
    
    private val log = logger<SessionQueryService>()
    
    fun getContentsSessionWithParticipants(streamerId: Long, pageable: Pageable): ContentsSessionResponseDto {
        val contentsSession = getOpenContentsSession(streamerId = streamerId)
        val participants = sessionRepository.findPagedParticipantsBySessionCode(contentsSession.sessionCode, pageable)
        log.info("참가자 페이지 결과: count={}", participants.totalElements)
        log.info("세션 조회 완료: sessionCode={}", contentsSession.sessionCode)
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
                ?: run {
                    log.info("게임 참여 코드가 존재하지 않음: sessionCode={}, viewerId={}", sessionCode, viewerId)
                    throw GameParticipationCodeNotFoundException()
                }
        log.info("게임 참여 코드 조회 성공: code={}", gameParticipationCode)
        return ContentsSessionResponseDto(gameParticipationCode = gameParticipationCode)
    }
    
    fun getOpenContentsSession(sessionCode: String? = null, streamerId: Long? = null): ContentsSession =
            sessionRepository.findOpenContentsSessionBy(sessionCode, streamerId) ?: throw NoOpenContentsSessionException()
    
    fun getParticipant(viewerId: Long, sessionId: Long): SessionParticipant =
            sessionRepository.findParticipantBy(viewerId, sessionId = sessionId) ?: throw ParticipantNotFoundException()
    
    fun existsParticipantInSession(sessionId: Long, viewerId: Long): Boolean =
            sessionRepository.existsParticipantInSession(sessionId, viewerId)
    
}