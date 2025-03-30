package com.chit.app.domain.sse.infrastructure

import com.chit.app.domain.member.application.MemberService
import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.application.service.ParticipantService
import com.chit.app.domain.session.domain.model.Participant
import com.chit.app.domain.session.domain.model.ParticipantOrderEvent
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.common.logging.logger
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture.*
import java.util.concurrent.ExecutorService

@Service
class SseAdapter(
        private val taskExecutor: ExecutorService,
        
        private val sseEmitterManager: SseEmitterManager,
        private val sessionRepository: SessionRepository,
        
        private val memberService: MemberService,
        private val participantService: ParticipantService
) {
    
    private val log = logger<SseAdapter>()
    
    @Transactional
    fun subscribe(memberId: Long, sessionCode: String, gameNickname: String? = null): SseEmitter {
        val emitter = sseEmitterManager.subscribe(memberId, sessionCode)
        sendInitialSseMessage(emitter, memberId, sessionCode)
        gameNickname?.let { registerParticipant(memberId, sessionCode, it) }
        return emitter
    }
    
    fun sendEvent(memberId: Long, sessionCode: String, sseEventType: SseEventType, data: Any?) {
        val emitter = sseEmitterManager.getEmitter(sessionCode, memberId) ?: return
        dispatchEvent(emitter, sseEventType, data, memberId, sessionCode)
    }
    
    fun broadcastEvent(sessionCode: String, sseEventType: SseEventType, data: Any?) {
        val sessionEmitters = sseEmitterManager.getEmittersBySession(sessionCode) ?: return
        val futures = sessionEmitters.map { (memberId, emitter) ->
            runAsync({
                dispatchEvent(emitter, sseEventType, data, memberId, sessionCode)
            }, taskExecutor)
        }
        allOf(*futures.toTypedArray()).join()
    }
    
    @Async
    fun emitStreamerSessionUpdateEventAsync(streamerId: Long, contentsSession: ContentsSession, updatedContentsSession: ContentsSessionResponseDto) {
        // 1. 스트리머에게 업데이트된 세션 이벤트 전송
        sendEvent(streamerId, updatedContentsSession.sessionCode!!, SseEventType.STREAMER_SESSION_UPDATED, updatedContentsSession)
        log.info("스트리머($streamerId)에게 세션 업데이트 이벤트 전송 션료")
        
        // 2. 참여자 순서 변경 이벤트 브로드캐스트
        notifyReorderedParticipants(SseEventType.STREAMER_SESSION_UPDATED, contentsSession)
    }
    
    @Async
    fun emitParticipantFixedEventAsync(viewerId: Long, contentsSession: ContentsSession) {
        // 1. 스트리머에게 참여자 고정 이벤트 전송
        notifyStreamerOfParticipantEvent(SseEventType.PARTICIPANT_FIXED_SESSION, contentsSession, viewerId)
        log.info("스트리머(${contentsSession.streamerId})에게 'PARTICIPANT_FIXED_SESSION' 이벤트 알림 전송 완료 (참여자: $viewerId)")
        
        // 2. 참여자 순서 변경 이벤트 브로드캐스트
        notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, contentsSession)
    }
    
    @Async
    fun emitKickEventAsync(viewerId: Long, contentsSession: ContentsSession) {
        // 1. 대상 참여자(시청자)에게 퇴장 이벤트 전송
        sendEvent(viewerId, contentsSession.sessionCode, SseEventType.KICKED_SESSION, null)
        log.info("회원($viewerId)에게 'KICKED_SESSION' 이벤트 전송 완료 (세션코드: ${contentsSession.sessionCode})")
        
        // 2. 스트리머에게 참여자 퇴장 이벤트 전송
        notifyStreamerOfParticipantEvent(SseEventType.PARTICIPANT_KICKED_SESSION, contentsSession, viewerId)
        log.info("스트리머(${contentsSession.streamerId})에게 'PARTICIPANT_KICKED_SESSION' 이벤트 알림 전송 완료 (참여자: $viewerId)")
        
        // 3. 참여자 순서 변경 이벤트 브로드캐스트
        notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, contentsSession)
    }
    
    @Async
    fun emitExitEventAsync(viewerId: Long, contentsSession: ContentsSession) {
        // 1. 참여자에게 세션 퇴장 이벤트 전송 & SSE 연결 해제
        sendEvent(viewerId, contentsSession.sessionCode, SseEventType.PARTICIPANT_LEFT_SESSION, null)
        sseEmitterManager.unsubscribe(contentsSession.sessionCode, viewerId)
        
        // 2. 스트리머에게 참여자 퇴장 이벤트 알림 & 참여자 순서 변경 브로드캐스트
        notifyStreamerOfParticipantEvent(SseEventType.PARTICIPANT_LEFT_SESSION, contentsSession, viewerId)
        notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, contentsSession)
    }
    
    fun notifyReorderedParticipants(eventType: SseEventType, contentsSession: ContentsSession) {
        val sessionCode = contentsSession.sessionCode
        val sessionEmitters = sseEmitterManager.getEmittersBySession(sessionCode) ?: return
        val sortedOrders = ParticipantOrderManager.getSortedParticipantOrders(sessionCode)
        
        val futures = sortedOrders.mapIndexed { order, participantOrder ->
            val emitter = sessionEmitters[participantOrder.viewerId]
            if (emitter != null) {
                runAsync({
                    val eventData = SseData(
                        message = eventType.message,
                        data = ParticipantOrderEvent.of(
                            order,
                            participantOrder,
                            contentsSession.gameParticipationCode,
                            contentsSession.maxGroupParticipants
                        )
                    )
                    runCatching {
                        emitter.send(eventType, eventData)
                    }.onFailure { e ->
                        log.error("회원(${participantOrder.viewerId})에게 순서 변경 이벤트 전송 실패: ${e.message}", e)
                        emitter.complete()
                    }
                }, taskExecutor)
            } else {
                completedFuture(null)
            }
        }
        
        allOf(*futures.toTypedArray()).join()
    }
    
    fun notifyStreamerOfParticipantEvent(
            eventType: SseEventType,
            contentsSession: ContentsSession,
            viewerId: Long
    ) {
        val sessionCode = contentsSession.sessionCode
        sseEmitterManager.getEmitter(sessionCode, contentsSession.streamerId!!)?.let { streamerEmitter ->
            val order = ParticipantOrderManager.getParticipantRank(sessionCode, viewerId)
            val chzzkNickname = memberService.getChzzkNickname(viewerId)
            val participant = sessionRepository.findParticipantBy(viewerId, sessionCode)
                    ?: throw IllegalArgumentException("해당 세션 참여 정보를 확인할 수 없습니다. 다시 시도해 주세요.")
            
            runAsync({
                streamerEmitter.send(
                    eventType, SseData(
                        message = eventType.message,
                        data = mapOf(
                            "maxGroupParticipants" to contentsSession.maxGroupParticipants,
                            "currentParticipants" to contentsSession.currentParticipants,
                            "participant" to Participant(
                                order = order,
                                round = participant.round,
                                fixedPick = participant.fixedPick,
                                status = participant.status,
                                viewerId = participant.viewerId,
                                participantId = participant.id!!,
                                chzzkNickname = chzzkNickname,
                                gameNickname = participant.gameNickname
                            )
                        )
                    )
                )
                log.info("스트리머(${contentsSession.streamerId})에게 참여자(${viewerId})의 이벤트 '${eventType.name}' 전송 완료")
            }, taskExecutor)
        }
    }
    
    private fun registerParticipant(viewerId: Long, sessionCode: String, gameNickname: String) {
        val joinSession = participantService.joinSession(sessionCode, viewerId, gameNickname)
        notifyStreamerOfParticipantEvent(SseEventType.PARTICIPANT_JOINED, joinSession, viewerId)
        notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, joinSession)
    }
    
    private fun dispatchEvent(emitter: SseEmitter, eventType: SseEventType, data: Any?, memberId: Long, sessionCode: String) {
        try {
            emitter.send(eventType, SseData(message = eventType.message, data = data))
            log.info("회원($memberId)에게 이벤트 '${eventType.name}' 전송 성공 (세션코드: $sessionCode)")
        } catch (e: Exception) {
            log.error("회원($memberId)에게 이벤트 전송 실패 (세션코드: $sessionCode): ${e.message}", e)
            emitter.complete()
        }
    }
    
    private fun sendInitialSseMessage(emitter: SseEmitter, memberId: Long, sessionCode: String) {
        try {
            emitter.send(SseEventType.JOINED_SESSION, SseData(message = SseEventType.JOINED_SESSION.message, data = sessionCode))
            log.info("회원($memberId)에게 초기 SSE 메시지 전송 성공 (세션코드: $sessionCode)")
        } catch (e: Exception) {
            log.error("회원($memberId) 초기 SSE 메시지 전송 실패 (세션코드: $sessionCode): ${e.message}", e)
            emitter.complete()
        }
    }
    
    private fun SseEmitter.send(eventType: SseEventType, data: SseData) {
        this.send(SseEmitter.event().name(eventType.name).data(data))
    }
    
    private enum class SseStatus { OK, FAIL, WARN }
    
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private data class SseData(
            val status: SseStatus = SseStatus.OK,
            val message: String?,
            val data: Any?
    )
}