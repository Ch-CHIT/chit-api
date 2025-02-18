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
class LiveStream private constructor(
        
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        val id: Long? = null,
        
        @Column(name = "live_id", nullable = false)
        val liveId: Long?,
        
        @Column(name = "streamer_id", nullable = false)
        val streamerId: Long?,
        
        @Column(name = "channel_id", nullable = false)
        val channelId: String?,
        
        @Column(name = "live_title")
        private var _liveTitle: String,
        
        @Enumerated(EnumType.STRING)
        @Column(name = "live_status")
        private var _liveStatus: LiveStatus,
        
        @Column(name = "category_type")
        private var _categoryType: String? = null,
        
        @Column(name = "live_category")
        private var _liveCategory: String? = null,
        
        @Column(name = "live_category_value")
        private var _liveCategoryValue: String? = null,
        
        @Column(name = "open_date")
        private var _openDate: LocalDateTime,
        
        @Column(name = "close_date")
        private var _closeDate: LocalDateTime? = null

) : BaseEntity() {
    
    var liveStatus: LiveStatus
        get() = _liveStatus
        set(value) {
            _liveStatus = value
        }
    
    var closedDate: LocalDateTime?
        get() = _closeDate
        set(value) {
            _closeDate = value
        }
    
    fun update(
            liveTitle: String,
            liveStatus: LiveStatus,
            categoryType: String?,
            liveCategory: String?,
            liveCategoryValue: String?,
            openDate: LocalDateTime,
            closeDate: LocalDateTime?
    ) {
        this._liveTitle = liveTitle
        this._liveStatus = liveStatus
        this._categoryType = categoryType
        this._liveCategory = liveCategory
        this._liveCategoryValue = liveCategoryValue
        this._openDate = openDate
        this._closeDate = closeDate
    }
    
    companion object {
        fun create(
                liveId: Long?,
                streamerId: Long?,
                channelId: String?,
                liveTitle: String,
                liveStatus: LiveStatus,
                categoryType: String?,
                liveCategory: String?,
                liveCategoryValue: String?,
                openDate: LocalDateTime,
                closeDate: LocalDateTime?
        ): LiveStream {
            return LiveStream(
                liveId = liveId,
                streamerId = streamerId,
                channelId = channelId,
                _liveTitle = liveTitle,
                _liveStatus = liveStatus,
                _categoryType = categoryType,
                _liveCategory = liveCategory,
                _liveCategoryValue = liveCategoryValue,
                _openDate = openDate,
                _closeDate = closeDate
            )
        }
    }
    
}