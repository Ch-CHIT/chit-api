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
        log.info("[성공] 세션 및 참여자 목록 조회 시작 (sessionCode={})", contentsSession.sessionCode)
        val participants = sessionRepository.findPagedParticipantsBySessionCode(contentsSession.sessionCode, pageable)
        log.info("[성공] 참여자 페이지 조회 완료 (sessionCode={}, count={})", contentsSession.sessionCode, participants.totalElements)
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
                    log.warn("[실패] 게임 참여 코드 조회 실패 - 존재하지 않음 (sessionCode={}, viewerId={})", sessionCode, viewerId)
                    throw GameParticipationCodeNotFoundException()
                }
        log.info("[성공] 게임 참여 코드 조회 완료 (sessionCode={}, viewerId={}, code={})", sessionCode, viewerId, gameParticipationCode)
        return ContentsSessionResponseDto(gameParticipationCode = gameParticipationCode)
    }
    
    fun getOpenContentsSession(sessionCode: String? = null, streamerId: Long? = null): ContentsSession {
        val session = sessionRepository.findOpenContentsSessionBy(sessionCode, streamerId)
        return session?.also {
            log.info("[성공] 오픈 세션 조회 완료 (sessionCode={}, streamerId={})", it.sessionCode, streamerId)
        } ?: run {
            log.warn("[실패] 오픈 세션 조회 실패 - 세션 없음 (sessionCode={}, streamerId={})", sessionCode, streamerId)
            throw NoOpenContentsSessionException()
        }
    }
    
    fun getParticipant(viewerId: Long, sessionId: Long): SessionParticipant {
        val participant = sessionRepository.findParticipantBy(viewerId, sessionId = sessionId)
        return participant?.also {
            log.info("[성공] 세션 참여자 조회 완료 (participantId={}, viewerId={}, sessionId={})", it.id, viewerId, sessionId)
        } ?: run {
            log.warn("[실패] 세션 참여자 조회 실패 - 참여자 없음 (viewerId={}, sessionId={})", viewerId, sessionId)
            throw ParticipantNotFoundException()
        }
    }
    
    fun existsParticipantInSession(sessionId: Long, viewerId: Long): Boolean {
        val exists = sessionRepository.existsParticipantInSession(sessionId, viewerId)
        log.info("[검증] 세션 내 참여자 존재 여부 확인 (sessionId={}, viewerId={}, exists={})", sessionId, viewerId, exists)
        return exists
    }
    
    fun hasOpenContentsSession(channelId: String?): Map<String, Boolean> {
        checkNotNull(channelId) { "유효하지 않은 channelId 입니다." }
        val isOpen = sessionRepository.existsOpenSessionByChannelId(channelId)
        return mapOf("isOpen" to isOpen)
    }
    
}