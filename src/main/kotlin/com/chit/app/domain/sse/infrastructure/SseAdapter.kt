package com.chit.app.domain.sse.infrastructure

import com.chit.app.domain.member.application.MemberQueryService
import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.domain.model.Participant
import com.chit.app.domain.session.domain.model.ParticipantOrderEvent
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.common.logging.logger
import com.fasterxml.jackson.annotation.JsonInclude
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import java.util.concurrent.CompletableFuture.*
import java.util.concurrent.ExecutorService

@Service
class SseAdapter(
        private val taskExecutor: ExecutorService,
        private val sseEmitterManager: SseEmitterManager,
        private val memberQueryService: MemberQueryService
) {
    
    private val log = logger<SseAdapter>()
    
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
        sendEvent(streamerId, updatedContentsSession.sessionCode!!, SseEventType.UPDATED_SESSION, updatedContentsSession)
        log.info("스트리머($streamerId)에게 세션 업데이트 이벤트 전송 션료")
        
        // 2. 참여자 순서 변경 이벤트 브로드캐스트
        notifyReorderedParticipants(SseEventType.UPDATED_SESSION, contentsSession)
    }
    
    @Async
    fun emitParticipantFixedEventAsync(participant: SessionParticipant) {
        val contentsSession = participant.contentsSession
        
        // 1. 스트리머에게 참여자 고정 이벤트 전송
        val order = ParticipantOrderManager.getParticipantRank(contentsSession.sessionCode, participant.viewerId)
        notifyStreamerOfParticipantEvent(SseEventType.PARTICIPANT_FIXED_SESSION, participant, order)
        log.info("스트리머(${contentsSession.streamerId})에게 'PARTICIPANT_FIXED_SESSION' 이벤트 알림 전송 완료 (참여자: $participant.viewerId)")
        
        // 2. 참여자 순서 변경 이벤트 브로드캐스트
        notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, contentsSession)
    }
    
    @Async
    fun emitKickEventAsync(participant: SessionParticipant) {
        val contentsSession = participant.contentsSession
        
        // 1. 대상 참여자(시청자)에게 퇴장 이벤트 전송
        sendEvent(participant.viewerId, contentsSession.sessionCode, SseEventType.KICKED_SESSION, null)
        sseEmitterManager.unsubscribe(contentsSession.sessionCode, participant.viewerId)
        log.info("회원($participant.viewerId)에게 'KICKED_SESSION' 이벤트 전송 완료 (세션코드: ${contentsSession.sessionCode})")
        
        // 2. 스트리머에게 참여자 퇴장 이벤트 전송
        val order = ParticipantOrderManager.removeParticipantOrderAndGetRank(contentsSession.sessionCode, participant.viewerId)
        notifyStreamerOfParticipantEvent(SseEventType.PARTICIPANT_KICKED_SESSION, participant, order)
        log.info("스트리머(${contentsSession.streamerId})에게 'PARTICIPANT_KICKED_SESSION' 이벤트 알림 전송 완료 (참여자: $participant.viewerId)")
        
        // 3. 참여자 순서 변경 이벤트 브로드캐스트
        notifyReorderedParticipants(SseEventType.SESSION_ORDER_UPDATED, contentsSession)
    }
    
    @Async
    fun emitExitEventAsync(participant: SessionParticipant) {
        val contentsSession = participant.contentsSession
        val sessionCode = contentsSession.sessionCode
        val viewerId = participant.viewerId
        
        // 1. 참여자에게 세션 퇴장 이벤트 전송 & SSE 연결 해제
        sendEvent(viewerId, sessionCode, SseEventType.LEFT_SESSION, null)
        log.info("회원($viewerId)에게 'LEFT_SESSION' 이벤트 전송 완료 (세션코드: $sessionCode)")
        
        sseEmitterManager.unsubscribe(sessionCode, viewerId)
        log.info("회원($viewerId)의 SSE 연결 해제 완료 (세션코드: $sessionCode)")
        
        // 2. 스트리머에게 참여자 퇴장 이벤트 알림 & 참여자 순서 변경 브로드캐스트
        val order = ParticipantOrderManager.removeParticipantOrderAndGetRank(contentsSession.sessionCode, participant.viewerId)
        notifyStreamerOfParticipantEvent(SseEventType.PARTICIPANT_LEFT_SESSION, participant, order)
        log.info("스트리머(${contentsSession.streamerId})에게 'PARTICIPANT_LEFT_SESSION' 이벤트 알림 전송 완료 (참여자: $viewerId)")
        
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
            participant: SessionParticipant,
            order: Int?,
    ) {
        val contentsSession = participant.contentsSession
        val sessionCode = contentsSession.sessionCode
        val viewerId = participant.viewerId
        
        sseEmitterManager.getEmitter(sessionCode, contentsSession.streamerId!!)?.let { streamerEmitter ->
            val chzzkNickname = memberQueryService.getMember(memberId = viewerId).channelName
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
                                viewerId = viewerId,
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
    
    private fun dispatchEvent(emitter: SseEmitter, eventType: SseEventType, data: Any?, memberId: Long, sessionCode: String) {
        try {
            emitter.send(eventType, SseData(message = eventType.message, data = data))
            log.info("회원($memberId)에게 이벤트 '${eventType.name}' 전송 성공 (세션코드: $sessionCode)")
        } catch (e: Exception) {
            log.error("회원($memberId)에게 이벤트 전송 실패 (세션코드: $sessionCode): ${e.message}", e)
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