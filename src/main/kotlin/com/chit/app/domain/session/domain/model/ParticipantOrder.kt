package com.chit.app.domain.session.domain.model

import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus

data class ParticipantOrder(
        val round: Int,
        val fixed: Boolean = false,
        val status: ParticipationStatus,
        val participantId: Long,
        val viewerId: Long
) : Comparable<ParticipantOrder> {
    
    override fun compareTo(other: ParticipantOrder): Int {
        val roundCompare = this.round.compareTo(other.round)
        if (roundCompare != 0) return roundCompare
        
        val fixedCompare = when {
            this.fixed == other.fixed -> 0
            this.fixed                -> -1
            else                      -> 1
        }
        if (fixedCompare != 0) return fixedCompare
        
        return this.participantId.compareTo(other.participantId)
    }
    
    fun nextCycle(): ParticipantOrder {
        return copy(round = round + 1)
    }
    
    companion object {
        fun of(participant: SessionParticipant, viewerId: Long): ParticipantOrder {
            return ParticipantOrder(
                round = participant.round,
                fixed = participant.fixedPick,
                status = participant.status,
                participantId = participant.id!!,
                viewerId = viewerId
            )
        }
    }
    
}