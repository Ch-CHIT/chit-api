package com.chit.app.domain.session.application.service

import com.chit.app.domain.live.domain.model.LiveStream
import com.chit.app.domain.live.domain.repository.LiveStreamRepository
import com.chit.app.domain.session.application.dto.ContentsSessionResponseDto
import com.chit.app.domain.session.application.dto.SseEvent
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.event.ParticipantDisconnectionEvent
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.domain.session.domain.service.ParticipantOrderManager
import com.chit.app.global.annotation.LogExecutionTime
import com.chit.app.global.common.logging.logger
import com.chit.app.global.common.response.SuccessResponse.PagedResponse
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.CompletableFuture.runAsync
import java.util.concurrent.ExecutorService

@Service
class SessionService(
        private val taskExecutor: ExecutorService,
        private val eventPublisher: ApplicationEventPublisher,
        
        private val sessionRepository: SessionRepository,
        private val sessionSseService: SessionSseService,
        private val streamerSseService: StreamerSseService,
        private val liveStreamRepository: LiveStreamRepository,
) {
    
    private val log = logger<SessionService>()
    
    /**
     * 스트리머의 라이브 방송이 진행 중인 상태에서, 새로운 컨텐츠 세션을 생성
     *
     * 1. 스트리머의 진행 중인 라이브 스트림 정보 조회
     * 2. 동일 라이브 방송에 진행 중인 세션이 존재하는지 체크하고, 중복 생성 시 예외 발생
     * 3. ContentsSession 객체를 생성하고 저장한 후, ResponseDto로 변환하여 반환
     */
    @LogExecutionTime
    @Transactional
    fun createContentsSession(streamerId: Long, gameParticipationCode: String?, maxGroupParticipants: Int): ContentsSessionResponseDto {
        // 현재 진행 중인 라이브 스트림 정보를 조회
        val openLiveStream = getOpenLiveStream(streamerId)
        
        // 동일 라이브 방송에 열린 컨텐츠 세션이 존재하면 중복 생성 불가
        check(sessionRepository.notExistsOpenContentsSession(openLiveStream.liveId!!)) {
            "이미 진행 중인 컨텐츠 세션이 존재합니다. 중복 생성을 할 수 없습니다."
        }
        
        // 새로운 컨텐츠 세션 생성 후 저장 및 ResponseDto 변환
        val contentsSession = ContentsSession.create(
            streamerId = streamerId,
            liveId = openLiveStream.liveId,
            maxGroupParticipants = maxGroupParticipants,
            gameParticipationCode = gameParticipationCode
        )
        return sessionRepository.save(contentsSession).toResponseDto()
    }
    
    /**
     * 스트리머의 현재 진행 중인 컨텐츠 세션과 해당 세션의 참가자 목록을 페이징하여 반환
     *
     * 1. 현재 열린 컨텐츠 세션 정보를 조회
     * 2. 해당 세션의 참가자 정보를 페이징 처리하여 조회
     * 3. 조회된 정보와 함께 ResponseDto를 생성하여 반환
     */
    @LogExecutionTime
    @Transactional(readOnly = true)
    fun getOpeningContentsSession(streamerId: Long, pageable: Pageable): ContentsSessionResponseDto {
        // 현재 열린 세션 정보 조회
        val session = getOpenContentsSessionByStreamerId(streamerId)
        
        // 페이징된 참가자 목록 조회
        val participants = sessionRepository.findPagedParticipantsBySessionCode(session.sessionCode, pageable)
        
        // ResponseDto 생성 및 반환
        return ContentsSessionResponseDto(
            sessionCode = session.sessionCode,
            gameParticipationCode = session.gameParticipationCode,
            maxGroupParticipants = session.maxGroupParticipants,
            currentParticipants = session.currentParticipants,
            participants = PagedResponse.from(participants)
        )
    }
    
    /**
     * 스트리머의 현재 진행 중인 컨텐츠 세션에 대해, 특정 참여자의 연결 해제 이벤트를 발행
     * -> viewerId가 null인 경우 예외 발생
     */
    @LogExecutionTime
    @Transactional
    fun publishDisconnectionNotification(streamerId: Long, viewerId: Long?) {
        // viewerId 유효성 체크
        require(viewerId != null) { "유효하지 않은 참여자 정보입니다." }
        
        // 현재 열린 세션 정보 조회
        val session = getOpenContentsSessionByStreamerId(streamerId)
        
        // 참여자 연결 해제 이벤트 발행
        eventPublisher.publishEvent(ParticipantDisconnectionEvent(session.sessionCode, viewerId))
    }
    
    /**
     * 스트리머의 현재 컨텐츠 세션을 종료하고, 관련 리소스 및 SSE 연결을 정리
     *
     * 1. 열린 세션을 종료 상태로 변경
     * 2. 해당 세션의 모든 참여자 상태를 'LEFT'로 업데이트
     * 3. 참가 순서 큐 초기화
     * 4. SSE 연결 종료 및 스트리머 구독 해제 작업을 비동기로 실행
     */
    @LogExecutionTime
    @Transactional
    fun closeContentsSession(streamerId: Long) {
        // 현재 열린 세션 정보를 조회하고 종료 처리
        val session = getOpenContentsSessionByStreamerId(streamerId).apply { close() }
        val sessionCode = session.sessionCode
        
        // 세션에 참여 중인 모든 참여자의 상태 업데이트 ('LEFT')
        sessionRepository.setAllParticipantsToLeft(sessionCode)
        
        // 참가 순서 큐 초기화
        ParticipantOrderManager.removeParticipantOrderQueue(sessionCode)
        
        // SSE 연결 종료 및 스트리머 구독 해제 처리
        runAsync({
            sessionSseService.disconnectAllSseEmitter(sessionCode)
            streamerSseService.unsubscribe(streamerId)
        }, taskExecutor)
    }
    
    /**
     * 특정 참여자의 고정 선택(fixed pick) 상태를 토글하고, 변경된 상태를 반영하여 순서를 업데이트
     * -> viewerId가 null이거나 해당 참여자를 찾지 못할 경우 예외 발생
     */
    @LogExecutionTime
    @Transactional
    fun switchFixedPickStatus(streamerId: Long, viewerId: Long?) {
        // viewerId 유효성 체크
        require(viewerId != null) { "유효하지 않은 참여자 정보입니다." }
        
        // 현재 열린 세션 정보 조회
        val session = getOpenContentsSessionByStreamerId(streamerId)
        
        // 특정 참여자 조회 후 고정 선택 상태 토글 -> 참가 순서 업데이트, 없으면 예외 발생
        sessionRepository.findParticipantBy(viewerId, sessionId = session.id!!)
                ?.apply { toggleFixedPick() }
                ?.also { ParticipantOrderManager.addOrUpdateParticipantOrder(session.sessionCode, it, viewerId) }
                ?: throw IllegalArgumentException("참여자를 찾을 수 없습니다. 해당 세션에 유효한 참가자가 존재하지 않습니다.")
        
        // 모든 참여자에게 순서 업데이트 이벤트 전송
        runAsync({
            sessionSseService.reorderSessionParticipants(
                SseEvent.PARTICIPANT_ORDER_UPDATED,
                session.sessionCode,
                session.gameParticipationCode,
                session.maxGroupParticipants
            )
        }, taskExecutor)
    }
    
    /**
     * 특정 세션과 참여자에 대한 게임 참여 코드를 조회하여 ResponseDto로 반환
     * -> 참여자 정보가 없으면 예외 발생
     */
    @LogExecutionTime
    @Transactional(readOnly = true)
    fun retrieveGameParticipationCode(sessionCode: String, participantId: Long): ContentsSessionResponseDto {
        // 세션 및 참여자 기반 게임 참여 코드 조회
        val gameParticipationCode = sessionRepository.findGameParticipationCodeBy(sessionCode, participantId)
                ?: throw IllegalArgumentException("해당 세션에 참가자 정보가 존재하지 않습니다. 다시 확인해 주세요.")
        
        // 조회된 코드를 포함한 ResponseDto 생성 및 반환
        return ContentsSessionResponseDto(gameParticipationCode = gameParticipationCode)
    }
    
    /**
     * 참여자가 세션을 종료할 때, 종료 이벤트 전송 및 SSE 연결 종료 처리
     */
    @LogExecutionTime
    @Transactional
    fun exitContentsSession(viewerId: Long, sessionCode: String) {
        sessionSseService.emitSessionCloseEvent(sessionCode, viewerId)
    }
    
    /**
     * 스트리머의 현재 세션에서 다음 그룹으로 진행
     *
     * 1. 현재 열린 세션 정보 조회
     * 2. 첫 그룹에 속한 참여자들의 라운드 증가
     * 3. 참여 순서의 사이클을 진행
     * 4. 비동기로 모든 참여자에게 순서 업데이트 이벤트 전송
     */
    @LogExecutionTime
    @Transactional
    fun proceedToNextGroup(streamerId: Long) {
        // 현재 열린 세션 정보 조회
        val session = getOpenContentsSessionByStreamerId(streamerId)
        
        // 첫 그룹 참여자들의 라운드 증가 및 순서 사이클 진행
        sessionRepository.findFirstPartyParticipants(session.id!!, session.maxGroupParticipants.toLong())
                .forEach { participant -> participant.incrementSessionRound() }
                .also { ParticipantOrderManager.advanceCycleForFirstNParticipantOrders(session) }
        
        // 모든 참여자에게 순서 업데이트 이벤트 전송
        runAsync({
            sessionSseService.reorderSessionParticipants(
                SseEvent.PARTICIPANT_ORDER_UPDATED,
                session.sessionCode,
                session.gameParticipationCode,
                session.maxGroupParticipants
            )
        }, taskExecutor)
    }
    
    /**
     * 스트리머 ID를 기반으로 현재 진행 중인 컨텐츠 세션 정보를 조회
     * -> 세션이 존재하지 않으면 예외 발생
     */
    private fun getOpenContentsSessionByStreamerId(streamerId: Long): ContentsSession {
        return sessionRepository.findOpenContentsSessionBy(streamerId = streamerId)
                ?: throw IllegalArgumentException("현재 진행 중인 시청자 참여 세션이 없습니다. 다시 확인해 주세요.")
    }
    
    /**
     * 스트리머 ID를 기반으로 현재 진행 중인 라이브 스트림 정보를 조회
     * -> 라이브 스트림이 존재하지 않으면 예외 발생
     */
    private fun getOpenLiveStream(streamerId: Long): LiveStream {
        return liveStreamRepository.findOpenLiveStreamBy(streamerId = streamerId)
                ?: throw IllegalArgumentException("현재 진행중인 라이브 방송을 찾을 수 없습니다. 다시 확인해 주세요.")
    }
    
    private fun ContentsSession.toResponseDto(): ContentsSessionResponseDto {
        return ContentsSessionResponseDto(
            sessionCode = sessionCode,
            gameParticipationCode = gameParticipationCode,
            maxGroupParticipants = maxGroupParticipants,
            currentParticipants = currentParticipants
        )
    }
}