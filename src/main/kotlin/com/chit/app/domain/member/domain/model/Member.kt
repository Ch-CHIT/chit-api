package com.chit.app.domain.member.domain.model

import com.chit.app.global.common.entity.BaseEntity
import jakarta.persistence.*
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import java.time.LocalDateTime

@Entity
@Table(
    name = "members",
    uniqueConstraints = [UniqueConstraint(name = "unq_members_channel_id", columnNames = ["channel_id"])]
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