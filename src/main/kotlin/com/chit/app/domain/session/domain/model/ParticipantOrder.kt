package com.chit.app.domain.session.domain.model

import com.chit.app.domain.session.domain.model.status.ParticipationStatus

data class ParticipantOrder(
        val cycle: Int = 1,
        val fixed: Boolean = false,
        val status: ParticipationStatus,
        val participantId: Long,
        val viewerId: Long
) : Comparable<ParticipantOrder> {
    override fun compareTo(other: ParticipantOrder): Int {
        // 1. cycle이 작은 참가자가 우선
        val cycleCompare = this.cycle.compareTo(other.cycle)
        if (cycleCompare != 0) return cycleCompare
        
        // 2. fixed가 true인 참가자가 우선
        val fixedCompare = when {
            this.fixed == other.fixed -> 0
            this.fixed                -> -1
            else                      -> 1
        }
        if (fixedCompare != 0) return fixedCompare
        
        // 3. participantId가 낮은 참가자가 우선
        return this.participantId.compareTo(other.participantId)
    }
}