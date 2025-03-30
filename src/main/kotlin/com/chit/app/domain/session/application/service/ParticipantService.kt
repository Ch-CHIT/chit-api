package com.chit.app.domain.session.application.service

import com.chit.app.domain.member.application.MemberService
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.domain.sse.infrastructure.SseAdapter
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ParticipantService(
        private val sseAdapter: SseAdapter,
        private val memberService: MemberService,
        private val sessionRepository: SessionRepository
) {
    
    private val log = logger<ParticipantService>()
    
    @Transactional
    fun joinSession(
            sessionCode: String,
            viewerId: Long,
            gameNickname: String
    ): ContentsSession {
        val contentsSession = sessionRepository.findOpenContentsSessionBy(sessionCode)
                ?: throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
        
        if (isParticipantNotJoined(contentsSession.id!!, viewerId)) {
            val participant = SessionParticipant.create(viewerId, gameNickname, contentsSession)
            participant.joinContentSession()
            log.info("참여자 등록 완료 - 시청자ID: $viewerId, 세션코드: $sessionCode")
        } else {
            log.info("이미 참여 중인 시청자입니다 : 시청자ID: $viewerId, 세션코드: $sessionCode")
        }
        
        return contentsSession
    }
    
    @Transactional
    fun leaveSession(sessionCode: String, viewerId: Long) {
        val participant = sessionRepository.findParticipantBy(viewerId, sessionCode)
                ?: throw IllegalArgumentException("해당 세션 참여 정보를 확인할 수 없습니다. 다시 시도해 주세요.")
        
        participant.leaveContentSession()
        sseAdapter.emitExitEventAsync(viewerId, participant.contentsSession)
        log.info("참여자 퇴장 처리 완료 - 시청자ID: $viewerId, 세션코드: $sessionCode")
    }
    
    @Transactional
    fun kickParticipant(streamerId: Long, viewerId: Long?) {
        require(viewerId != null) { "유효하지 않은 참여자 정보입니다." }
        val participant = sessionRepository.findParticipantBy(viewerId, streamerId = streamerId)
                ?.apply { leaveContentSession() }
                ?: throw IllegalArgumentException("해당 세션 참여 정보를 확인할 수 없습니다. 다시 시도해 주세요.")
        
        sseAdapter.emitKickEventAsync(viewerId, participant.contentsSession)
    }
    
    private fun isParticipantNotJoined(sessionId: Long, viewerId: Long): Boolean {
        val notJoined = !sessionRepository.existsParticipantInSession(sessionId, viewerId)
        return notJoined
    }
    
    private fun SessionParticipant.joinContentSession() {
        sessionRepository.addParticipant(this)
        contentsSession.addParticipant()
        ParticipantOrderManager.addOrUpdateParticipantOrder(
            contentsSession.sessionCode,
            this,
            viewerId,
            memberService.getChzzkNickname(viewerId)
        )
    }
    
    private fun SessionParticipant.leaveContentSession() {
        status = ParticipationStatus.LEFT
        contentsSession.removeParticipant()
    }
    
}