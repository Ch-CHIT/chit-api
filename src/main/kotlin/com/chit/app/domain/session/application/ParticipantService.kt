package com.chit.app.domain.session.application

import com.chit.app.domain.session.domain.model.SessionParticipant
import com.chit.app.domain.session.domain.repository.SessionRepository
import com.chit.app.global.delegate.logger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ParticipantService(
        private val sessionRepository: SessionRepository
) {
    
    private val log = logger<ParticipantService>()
    
    @Transactional
    fun joinSession(sessionCode: String, participantId: Long, gameNickname: String) {
        sessionRepository.findBySessionCode(sessionCode)
                ?.apply {
                    val sessionParticipant = SessionParticipant.create(participantId, gameNickname, contentsSession = this)
                    sessionRepository.addParticipant(sessionParticipant)
                    log.info("참여자 추가 완료: participantId={}, gameNickname={}", participantId, gameNickname)
                }
                ?: run {
                    log.error("세션 참여 실패: sessionCode={}에 해당하는 세션을 찾을 수 없습니다.", sessionCode)
                    throw IllegalArgumentException("입력하신 세션 참여 코드를 가진 세션을 찾을 수 없습니다. 다시 확인해 주세요.")
                }
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