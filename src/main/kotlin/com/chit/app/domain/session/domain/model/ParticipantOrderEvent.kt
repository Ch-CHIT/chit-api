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
        val isReadyToPlay: Boolean,
        val gameParticipationCode: String?,
) {
    companion object {
        fun of(
                order: Int,
                participantOrder: ParticipantOrder,
                gameParticipationCode: String?,
                maxGroupParticipants: Int
        ): ParticipantOrderEvent {
            val eventOrder = order + 1
            val isPendingValue = eventOrder <= maxGroupParticipants
            return ParticipantOrderEvent(
                order = eventOrder,
                round = participantOrder.round,
                fixedPick = participantOrder.fixedPick,
                status = participantOrder.status,
                viewerId = participantOrder.viewerId,
                participantId = participantOrder.participantId,
                chzzkNickname = participantOrder.chzzkNickname,
                gameNickname = participantOrder.gameNickname,
                isReadyToPlay = isPendingValue,
                gameParticipationCode = if (isPendingValue && !gameParticipationCode.isNullOrEmpty()) gameParticipationCode else null
            )
        }
    }
}