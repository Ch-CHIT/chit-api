package com.chit.app.domain.session.domain.service

import com.chit.app.domain.session.domain.model.ParticipantOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object ParticipantOrderManager {
    
    private val sessionQueues: ConcurrentHashMap<String, ConcurrentSkipListSet<ParticipantOrder>> = ConcurrentHashMap()
    
    fun getParticipantsSorted(sessionCode: String): List<ParticipantOrder> {
        return sessionQueues[sessionCode]
                ?.let { queue -> synchronized(queue) { queue.toList() } }
                ?: emptyList()
    }
    
    fun addParticipant(sessionCode: String, participantOrder: ParticipantOrder) {
        val queue = sessionQueues.computeIfAbsent(sessionCode) { ConcurrentSkipListSet() }
        synchronized(queue) {
            queue.add(participantOrder)
        }
    }
    
    fun removeParticipantByViewerId(sessionCode: String, viewerId: Long): Boolean {
        val queue = sessionQueues[sessionCode] ?: return false
        return synchronized(queue) {
            queue.removeIf { it.viewerId == viewerId }
        }
    }
    
    fun moveFirstNParticipantsToNextCycle(sessionCode: String, count: Int) {
        val queue = sessionQueues[sessionCode] ?: return
        synchronized(queue) {
            val snapshot = queue.toList()
            snapshot.take(count).forEach { participant ->
                updateParticipant(sessionCode, participant.copy(cycle = participant.cycle + 1))
            }
        }
    }
    
    fun updateParticipant(sessionCode: String, participantOrder: ParticipantOrder) {
        val queue = sessionQueues.computeIfAbsent(sessionCode) { ConcurrentSkipListSet() }
        synchronized(queue) {
            queue.removeIf { it.viewerId == participantOrder.viewerId }
            queue.add(participantOrder)
        }
    }
    
    fun removeSession(sessionCode: String) {
        sessionQueues.remove(sessionCode)
    }
    
}