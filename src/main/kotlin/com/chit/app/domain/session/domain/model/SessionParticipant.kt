package com.chit.app.domain.session.domain.model

import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

@Entity
@Table(
    name = "session_participant",
    indexes = [Index(name = "idx_member_session", columnList = "viewer_id, contents_session_id", unique = true)]
)
class SessionParticipant private constructor(
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,
        
        @field:NotNull(message = "콘텐츠 세션을 찾을 수 없습니다.")
        @ManyToOne(fetch = FetchType.LAZY)
        @JoinColumn(name = "contents_session_id", nullable = false)
        val contentsSession: ContentsSession,
        
        @field:NotNull(message = "회원 ID가 존재하지 않습니다.")
        @Column(name = "viewer_id", nullable = false)
        val viewerId: Long,
        
        @field:NotEmpty(message = "프로필 닉네임을 찾을 수 없습니다.")
        @Column(name = "game_nickname", nullable = false)
        private var _gameNickname: String,
        
        @field:NotNull
        @Enumerated(EnumType.STRING)
        @Column(name = "status", nullable = false)
        private var _status: ParticipationStatus = ParticipationStatus.PENDING,
        
        @Column(name = "fixed_pick", nullable = false)
        private var _fixedPick: Boolean = false,
        
        @Column(name = "fixed_pick_time")
        private var _fixedPickTime: LocalDateTime? = null

) : BaseEntity() {
    
    val gameNickname: String
        get() = _gameNickname
    
    val status: ParticipationStatus
        get() = _status
    
    val fixedPick: Boolean
        get() = _fixedPick
    
    val fixedPickTime: LocalDateTime?
        get() = _fixedPickTime
    
    fun updateStatus(status: ParticipationStatus) {
        require(_status.canTransitionTo(status)) {
            "현재 상태에서 $status 로 전환할 수 없습니다."
        }
        _status = status
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