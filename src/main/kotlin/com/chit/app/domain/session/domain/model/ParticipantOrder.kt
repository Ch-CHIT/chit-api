package com.chit.app.domain.session.domain.model

import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus

data class ParticipantOrder(
        val viewerId: Long,
        val participantId: Long,
        val round: Int,
        val fixedPick: Boolean,
        val chzzkNickname: String,
        val gameNickname: String,
        val status: ParticipationStatus
) : Comparable<ParticipantOrder> {
    
    override fun compareTo(other: ParticipantOrder): Int {
        val roundCompare = this.round.compareTo(other.round)
        if (roundCompare != 0) return roundCompare
        
        val fixedCompare = when {
            this.fixedPick == other.fixedPick -> 0
            this.fixedPick                    -> -1
            else                              -> 1
        }
        if (fixedCompare != 0) return fixedCompare
        
        return this.participantId.compareTo(other.participantId)
    }
    
    fun nextCycle(): ParticipantOrder {
        return copy(round = round + 1)
    }
    
    companion object {
        fun of(participant: SessionParticipant, viewerId: Long, chzzkNickname: String): ParticipantOrder {
            return ParticipantOrder(
                viewerId,
                participant.id!!,
                participant.round,
                participant.fixedPick,
                chzzkNickname,
                participant.gameNickname,
                participant.status
            )
        }
    }
    
}