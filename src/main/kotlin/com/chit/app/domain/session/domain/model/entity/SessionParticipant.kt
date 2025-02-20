package com.chit.app.domain.session.domain.model.entity

import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "session_participant",
    indexes = [Index(name = "idx_participant_session", columnList = "viewer_id, contents_session_id")]
)
class SessionParticipant private constructor(
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,
        
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "contents_session_id", nullable = false)
        val contentsSession: ContentsSession,
        
        @Column(name = "viewer_id", nullable = false)
        val viewerId: Long,
        
        @Column(name = "game_nickname", nullable = false)
        private var _gameNickname: String,
        
        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        private var _status: ParticipationStatus = ParticipationStatus.JOINED,
        
        @Column(name = "fixed_pick", nullable = false)
        private var _fixedPick: Boolean = false,
        
        @Column(name = "fixed_pick_time")
        private var _fixedPickTime: LocalDateTime? = null

) : BaseEntity() {
    
    val fixedPick: Boolean
        get() = _fixedPick
    
    var status: ParticipationStatus
        get() = _status
        set(value) {
            _status = value
        }
    
    fun toggleFixedPick() {
        _fixedPick = !_fixedPick
        _fixedPickTime = if (_fixedPick) LocalDateTime.now() else null
    }
    
    companion object {
        fun create(viewerId: Long, gameNickname: String, contentsSession: ContentsSession): SessionParticipant {
            return SessionParticipant(
                viewerId = viewerId,
                contentsSession = contentsSession,
                _gameNickname = gameNickname
            )
        }
    }
}