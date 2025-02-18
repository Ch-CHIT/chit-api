package com.chit.app.domain.member.domain.model

import com.chit.app.global.entity.BaseEntity
import jakarta.persistence.Cacheable
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.LocalDateTime

@Entity
@Table(
    name = "members",
    indexes = [
        Index(name = "idx_members_channel_id_unique", columnList = "channel_id", unique = true),
        Index(name = "idx_members_channel_name_unique", columnList = "channel_name", unique = true)
    ]
)
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "memberCache")
class Member private constructor(
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,
        
        @Column(name = "channel_id", nullable = false, length = 32)
        val channelId: String,
        
        @Column(name = "channel_name", nullable = false, length = 30)
        val channelName: String,
        
        @Column(name = "nick_name", nullable = false, length = 30)
        private var _nickname: String,
        
        @Column(name = "last_login_time")
        private var _lastLoginTime: LocalDateTime? = null

) : BaseEntity() {
    
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