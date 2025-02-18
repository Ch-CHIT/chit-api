package com.chit.app.domain.session.domain.service

import com.chit.app.domain.session.domain.model.ParticipantOrder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object ParticipantOrderManager {
    
    private val sessionQueues: ConcurrentHashMap<String, ConcurrentSkipListSet<ParticipantOrder>> = ConcurrentHashMap()
    
    fun addParticipant(sessionCode: String, participantOrder: ParticipantOrder) {
        val queue = sessionQueues.computeIfAbsent(sessionCode) { ConcurrentSkipListSet() }
        queue.add(participantOrder)
    }
    
    fun removeParticipant(sessionCode: String, viewerId: Long): Boolean {
        return sessionQueues[sessionCode]?.removeIf { it.viewerId == viewerId } == true
    }
    
    fun updateParticipant(sessionCode: String, participantOrder: ParticipantOrder) {
        sessionQueues.computeIfPresent(sessionCode) { _, queue ->
            queue.removeIf { it.viewerId == participantOrder.viewerId }
            queue.add(participantOrder)
            queue
        } ?: run {
            val newQueue = ConcurrentSkipListSet<ParticipantOrder>()
            newQueue.add(participantOrder)
            sessionQueues.putIfAbsent(sessionCode, newQueue)
        }
    }
    
    fun getSortedParticipants(sessionCode: String): List<ParticipantOrder> {
        return sessionQueues[sessionCode]
                ?.let { queue -> synchronized(queue) { queue.toList() } }
                ?: emptyList()
    }
    
    fun removeSession(sessionCode: String) {
        sessionQueues.remove(sessionCode)
    }
    
}