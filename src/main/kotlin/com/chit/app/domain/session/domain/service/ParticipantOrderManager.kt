package com.chit.app.domain.session.domain.service

import com.chit.app.domain.session.domain.model.ParticipantOrder
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object ParticipantOrderManager {
    
    private val participantOrderMap: ConcurrentHashMap<String, ConcurrentSkipListSet<ParticipantOrder>> = ConcurrentHashMap()
    
    fun getSortedParticipantOrders(sessionCode: String): List<ParticipantOrder> {
        return participantOrderMap[sessionCode]
                ?.let { queue -> synchronized(queue) { queue.toList() } }
                ?: emptyList()
    }
    
    fun addOrUpdateParticipantOrder(sessionCode: String, participant: SessionParticipant, viewerId: Long) {
        update(sessionCode, ParticipantOrder.of(participant, viewerId))
    }
    
    fun removeParticipantOrder(sessionCode: String, viewerId: Long): Boolean {
        val queue = participantOrderMap[sessionCode] ?: return false
        return synchronized(queue) { queue.removeIf { it.viewerId == viewerId } }
    }
    
    fun advanceCycleForFirstNParticipantOrders(session: ContentsSession) {
        val queue = participantOrderMap[session.sessionCode] ?: return
        synchronized(queue) {
            val snapshot = queue.toList()
            snapshot.take(session.maxGroupParticipants).forEach { participantOrder ->
                update(session.sessionCode, participantOrder.nextCycle())
            }
        }
    }
    
    fun removeParticipantOrderQueue(sessionCode: String) {
        participantOrderMap.remove(sessionCode)
    }
    
    private fun update(sessionCode: String, participantOrder: ParticipantOrder) {
        val queue = participantOrderMap.computeIfAbsent(sessionCode) { ConcurrentSkipListSet() }
        synchronized(queue) {
            queue.removeIf { it.viewerId == participantOrder.viewerId }
            queue.add(participantOrder)
        }
    }
    
}