package com.chit.app.domain.live.domain.model

import com.chit.app.global.entity.BaseEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(
    name = "live_streams",
    indexes = [
        Index(name = "idx_live_streams_live_id_unq", columnList = "live_id", unique = true),
        Index(name = "idx_live_streams_channel_id", columnList = "channel_id"),
        Index(name = "idx_live_streams_streamer_id", columnList = "streamer_id")
    ]
)
class LiveStream(
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,
        
        @Column(name = "live_id", nullable = false)
        val liveId: Long,
        
        @Column(name = "streamer_id", nullable = false)
        val streamerId: Long,
        
        @Column(name = "channel_id", nullable = false)
        val channelId: String,
        
        @Column(name = "live_title")
        private var _liveTitle: String,
        
        @Column(name = "live_status")
        private var _liveStatus: LiveStatus,
        
        @Column(name = "category_type")
        private var _categoryType: String,
        
        @Column(name = "live_category")
        private var _liveCategory: String,
        
        @Column(name = "live_category_value")
        private var _liveCategoryValue: String,
        
        @Column(name = "open_date")
        private var _openDate: LocalDateTime,
        
        @Column(name = "close_date")
        private var _closeDate: LocalDateTime? = null

) : BaseEntity() {
    
    val liveTitle: String
        get() = _liveTitle
    
    val liveStatus: LiveStatus
        get() = _liveStatus
    
    val categoryType: String
        get() = _categoryType
    
    val liveCategory: String
        get() = _liveCategory
    
    val liveCategoryValue: String
        get() = _liveCategoryValue
    
    val openDate: LocalDateTime
        get() = _openDate
    
    val closeDate: LocalDateTime?
        get() = _closeDate
    
}