package com.chit.app.domain.sse.application

import com.chit.app.domain.member.domain.repository.MemberRepository
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.repository.SessionRepository
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
        private val sessionRepository: SessionRepository,
        private val memberRepository: MemberRepository
) {
    
    private val log = logger<SseSubscribeService>()
    
    @Transactional
    fun subscribe(memberId: Long, sessionCode: String, gameNickname: String? = null): SseEmitter {
        validateOpenSession(sessionCode)
        val emitter = sseEmitterManager.subscribe(memberId, sessionCode)
        sseAdapter.sendEvent(memberId, sessionCode, SseEventType.JOINED_SESSION, sessionCode)
        gameNickname?.let { registerParticipant(memberId, sessionCode, it) }
        return emitter
    }
    
    private fun validateOpenSession(sessionCode: String): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(sessionCode)
                ?: throw IllegalArgumentException("해당 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
    }
    
    private fun registerParticipant(viewerId: Long, sessionCode: String, gameNickname: String) {
        val participant = joinSession(sessionCode, viewerId, gameNickname)
        val order = ParticipantOrderManager.getParticipantRank(sessionCode, participant.viewerId)
        sseAdapter.notifyStreamerOfParticipantEvent(SseEventType.PARTICIPANT_JOINED_SESSION, participant, order)
        sseAdapter.notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, participant.contentsSession)
    }
    
    private fun joinSession(sessionCode: String, viewerId: Long, gameNickname: String): SessionParticipant {
        val contentsSession = sessionRepository.findOpenContentsSessionBy(sessionCode)
                ?: throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
        
        val participant: SessionParticipant =
                if (isParticipantNotJoined(contentsSession.id!!, viewerId)) {
                    val newParticipant = SessionParticipant.create(viewerId, gameNickname, contentsSession)
                    newParticipant.joinContentSession()
                    log.info("참여자 등록 완료 - 시청자ID: $viewerId, 세션코드: $sessionCode")
                    newParticipant
                } else {
                    log.info("이미 참여 중인 시청자입니다 : 시청자ID: $viewerId, 세션코드: $sessionCode")
                    sessionRepository.findParticipantBy(viewerId, sessionCode)
                            ?: throw IllegalStateException("참여 중이지만 참여자 정보를 조회할 수 없습니다.")
                }
        
        return participant
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
            getChzzkNickname()
        )
    }
    
    private fun SessionParticipant.getChzzkNickname(): String = memberRepository.findBy(memberId = viewerId)?.channelName
            ?: throw IllegalArgumentException("참여자 정보를 찾을 수 없습니다. 다시 시도해 주세요.")
    
}