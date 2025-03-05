package com.chit.app.domain.session.domain.model

import com.chit.app.domain.session.domain.model.status.ParticipationStatus

data class ParticipantOrderEvent(
        val order: Int,
        val round: Int,
        val fixedPick: Boolean,
        val status: ParticipationStatus,
        val viewerId: Long,
        val participantId: Long,
        val chzzkNickname: String,
        val gameNickname: String,
        val gameParticipationCode: String?,
) {
    companion object {
        fun of(
                order: Int,
                participantOrder: ParticipantOrder,
                gameParticipationCode: String?,
                maxGroupParticipants: Int
        ): ParticipantOrderEvent {
            return ParticipantOrderEvent(
                order = order + 1,
                round = participantOrder.round,
                fixedPick = participantOrder.fixedPick,
                status = participantOrder.status,
                viewerId = participantOrder.viewerId,
                participantId = participantOrder.participantId,
                chzzkNickname = participantOrder.chzzkNickname,
                gameNickname = participantOrder.gameNickname,
                gameParticipationCode = gameParticipationCode?.takeIf { order + 1 <= maxGroupParticipants && it.isNotEmpty() }
            )
        }
    }
}