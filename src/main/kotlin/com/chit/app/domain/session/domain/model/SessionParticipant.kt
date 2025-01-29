package com.chit.app.domain.session.domain.model

import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "session_participant",
    indexes = [Index(name = "idx_participant_session_unique", columnList = "participant_id, contents_session_id", unique = true)]
)
class SessionParticipant private constructor(
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,
        
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "contents_session_id", nullable = false)
        val contentsSession: ContentsSession,
        
        @Column(name = "participant_id", nullable = false)
        val participantId: Long,
        
        @Column(name = "game_nickname", nullable = false)
        private var _gameNickname: String,
        
        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        private var _status: ParticipationStatus = ParticipationStatus.PENDING,
        
        @Column(name = "fixed_pick", nullable = false)
        private var _fixedPick: Boolean = false,
        
        @Column(name = "fixed_pick_time")
        private var _fixedPickTime: LocalDateTime? = null

) : BaseEntity() {
    
    val fixedPick: Boolean
        get() = _fixedPick
    
    fun updateStatus(status: ParticipationStatus) {
        require(_status.canTransitionTo(status)) { "현재 상태에서 $status 로 전환할 수 없습니다." }
        _status = status
    }
    
    fun toggleFixedPick() {
        _fixedPick = !_fixedPick
        _fixedPickTime = if (_fixedPick) LocalDateTime.now() else null
    }
    
    companion object {
        fun create(participantId: Long, gameNickname: String, contentsSession: ContentsSession): SessionParticipant {
            return SessionParticipant(
                participantId = participantId,
                contentsSession = contentsSession,
                _gameNickname = gameNickname
            )
        }
    }
}