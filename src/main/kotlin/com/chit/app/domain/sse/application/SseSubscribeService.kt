package com.chit.app.domain.sse.application

import com.chit.app.domain.member.application.MemberQueryService
import com.chit.app.domain.session.application.service.SessionCommandService
import com.chit.app.domain.session.application.service.SessionQueryService
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.domain.sse.infrastructure.SseAdapter
import com.chit.app.domain.sse.infrastructure.SseEmitterManager
import com.chit.app.domain.sse.infrastructure.SseEventType
import com.chit.app.global.common.logging.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter

@Service
class SseSubscribeService(
        private val sseAdapter: SseAdapter,
        private val sseEmitterManager: SseEmitterManager,
        private val memberQueryService: MemberQueryService,
        private val sessionQueryService: SessionQueryService,
        private val sessionCommandService: SessionCommandService
) {
    
    private val log = logger<SseSubscribeService>()
    
    @Transactional
    fun subscribe(memberId: Long, sessionCode: String, gameNickname: String? = null): SseEmitter {
        sessionQueryService.getOpenContentsSession(sessionCode = sessionCode)
        val emitter = sseEmitterManager.subscribe(memberId, sessionCode)
        sseAdapter.sendEvent(memberId, sessionCode, SseEventType.JOINED_SESSION, sessionCode)
        gameNickname?.let { registerParticipant(memberId, sessionCode, it) }
        return emitter
    }
    
    private fun registerParticipant(viewerId: Long, sessionCode: String, gameNickname: String) {
        val participant = joinSession(sessionCode, viewerId, gameNickname)
        val order = ParticipantOrderManager.getParticipantRank(sessionCode, participant.viewerId)
        sseAdapter.notifyStreamerOfParticipantEvent(SseEventType.PARTICIPANT_JOINED_SESSION, participant, order)
        sseAdapter.notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, participant.contentsSession)
    }
    
    private fun joinSession(sessionCode: String, viewerId: Long, gameNickname: String): SessionParticipant {
        val contentsSession = sessionQueryService.getOpenContentsSession(sessionCode = sessionCode)
        val participant: SessionParticipant =
                if (isParticipantNotJoined(contentsSession.id!!, viewerId)) {
                    val newParticipant = SessionParticipant.create(viewerId, gameNickname, contentsSession)
                    newParticipant.joinContentSession()
                    log.info("참여자 등록 완료 - 시청자ID: $viewerId, 세션코드: $sessionCode")
                    newParticipant
                } else {
                    log.info("이미 참여 중인 시청자입니다 : 시청자ID: $viewerId, 세션코드: $sessionCode")
                    sessionQueryService.getParticipant(viewerId = viewerId, sessionId = contentsSession.id!!)
                }
        
        return participant
    }
    
    private fun isParticipantNotJoined(sessionId: Long, viewerId: Long): Boolean {
        val notJoined = !sessionQueryService.existsParticipantInSession(sessionId, viewerId)
        return notJoined
    }
    
    private fun SessionParticipant.joinContentSession() {
        sessionCommandService.addParticipantToSession(this)
        contentsSession.addParticipant()
        ParticipantOrderManager.addOrUpdateParticipantOrder(
            contentsSession.sessionCode,
            this,
            viewerId,
            memberQueryService.getMember(memberId = viewerId).channelName
        )
    }
    
}