package com.chit.app.domain.member.domain.model

import com.chit.app.global.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(
    name = "members",
    indexes = [
        Index(name = "idx_members_channel_id_unique", columnList = "channel_id", unique = true)
    ]
)
class Member private constructor(
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,
        
        @Column(name = "channel_id", nullable = false)
        val channelId: String,
        
        @Column(name = "channel_name", nullable = false)
        val channelName: String,
        
        @Column(name = "nick_name", nullable = false)
        private var _nickname: String,
        
        @Column(name = "last_login_time")
        private var _lastLoginTime: LocalDateTime? = null

) : BaseEntity() {
    
    val nickname: String
        get() = _nickname
    
    val lastLoginTime: LocalDateTime?
        get() = _lastLoginTime
    
    fun updateLastLoginTime() {
        this._lastLoginTime = LocalDateTime.now()
    }
    
    companion object {
        fun create(
                channelId: String,
                channelName: String,
                nickname: String
        ): Member = Member(
            channelId = channelId,
            channelName = channelName,
            _nickname = nickname
        )
    }
    
}