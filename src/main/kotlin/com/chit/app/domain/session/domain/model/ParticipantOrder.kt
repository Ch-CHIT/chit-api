package com.chit.app.domain.session.domain.model

import com.chit.app.domain.session.domain.model.status.ParticipationStatus

data class ParticipantOrder(
        val fixed: Boolean,
        val status: ParticipationStatus,
        val participantId: Long,
        val viewerId: Long
) : Comparable<ParticipantOrder> {
    override fun compareTo(other: ParticipantOrder): Int {
        // fixed: true가 우선
        val fixedCompare = when {
            this.fixed == other.fixed -> 0
            this.fixed                -> -1
            else                      -> 1
        }
        if (fixedCompare != 0) return fixedCompare
        
        // status: 낮은 order 값이 우선
        val statusCompare = this.status.order.compareTo(other.status.order)
        if (statusCompare != 0) return statusCompare
        
        // participantId: 낮은 값이 우선
        return this.participantId.compareTo(other.participantId)
    }
}