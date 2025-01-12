package com.chit.app.domain.session.domain.model

import com.chit.app.domain.session.domain.model.status.SessionStatus
import com.chit.app.global.entity.BaseEntity
import com.chit.app.global.util.NanoIdUtil
import jakarta.persistence.*

@Entity
@Table(
    name = "contents_sessions",
    indexes = [
        Index(name = "idx_contents_sessions_live_id_unq", columnList = "live_id", unique = true),
        Index(name = "idx_contents_sessions_streamer_id", columnList = "streamer_id")
    ]
)
class ContentsSession private constructor(
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,
        
        @Column(name = "live_id", nullable = false)
        val liveId: Long,
        
        @Column(name = "streamer_id", nullable = false)
        val streamerId: Long,
        
        @Column(name = "session_code", nullable = false)
        val sessionCode: String = NanoIdUtil.generate(),
        
        @Column(name = "status", nullable = false)
        private var _status: SessionStatus = SessionStatus.OPEN,
        
        @Column(name = "game_participation_code", length = 100)
        private var _gameParticipationCode: String? = null,
        
        @Column(name = "max_participants", nullable = false)
        private var _maxParticipants: Int = 1,
        
        @Column(name = "current_participants", nullable = false)
        private var _currentParticipants: Int = 1

) : BaseEntity() {
    
    val status: SessionStatus
        get() = _status
    
    val gameParticipationCode: String?
        get() = _gameParticipationCode
    
    val maxParticipants: Int
        get() = _maxParticipants
    
    val currentParticipants: Int
        get() = _currentParticipants
    
    fun updateGameDetails(
            gameParticipationCode: String,
            maxParticipantCount: Int
    ): ContentsSession = apply {
        require(maxParticipantCount >= 1) {
            "최대 참가자 수는 1명 이상이어야 합니다. 현재 값: $maxParticipantCount"
        }
        validateSessionIsOpen()
        this._gameParticipationCode = gameParticipationCode
        this._maxParticipants = maxParticipantCount
    }
    
    @Synchronized
    fun addParticipant(): ContentsSession = apply {
        validateSessionIsOpen()
        this._currentParticipants++
    }
    
    fun close(): ContentsSession = apply {
        validateSessionIsOpen()
        this._status = SessionStatus.CLOSE
    }
    
    private fun validateSessionIsOpen() = check(_status == SessionStatus.CLOSE) { "세션이 이미 종료되었습니다. 세션 ID: $id" }
    
    companion object {
        fun create(
                liveId: Long,
                streamerId: Long,
                maxParticipants: Int = 1,
                gameParticipationCode: String?
        ): ContentsSession {
            require(maxParticipants >= 1) {
                "최대 참가자 수는 1명 이상이어야 합니다. 현재 값: $maxParticipants"
            }
            
            return ContentsSession(
                liveId = liveId,
                streamerId = streamerId,
                _maxParticipants = maxParticipants,
                _gameParticipationCode = gameParticipationCode
            )
        }
    }
}