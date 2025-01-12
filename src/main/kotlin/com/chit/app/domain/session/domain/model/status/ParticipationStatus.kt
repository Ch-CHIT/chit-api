package com.chit.app.domain.session.domain.model.status

enum class ParticipationStatus {
    PENDING {
        override fun canTransitionTo(status: ParticipationStatus): Boolean {
            return status == APPROVED || status == REJECTED
        }
    },
    APPROVED {
        override fun canTransitionTo(status: ParticipationStatus): Boolean {
            return status == PENDING || status == REJECTED
        }
    },
    REJECTED {
        override fun canTransitionTo(status: ParticipationStatus): Boolean {
            return false
        }
    };
    
    abstract fun canTransitionTo(status: ParticipationStatus): Boolean
}