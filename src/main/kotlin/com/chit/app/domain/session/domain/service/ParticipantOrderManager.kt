package com.chit.app.domain.session.domain.service

import com.chit.app.domain.session.domain.model.ParticipantOrder
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object ParticipantOrderManager {
    
    private val participantOrderMap: ConcurrentHashMap<String, ConcurrentSkipListSet<ParticipantOrder>> = ConcurrentHashMap()
    
    fun addOrUpdateParticipantOrder(sessionCode: String, participant: SessionParticipant, viewerId: Long, chzzkNickname: String) {
        update(sessionCode, ParticipantOrder.of(participant, viewerId, chzzkNickname))
    }
    
    fun getSortedParticipantOrders(sessionCode: String): List<ParticipantOrder> {
        return participantOrderMap[sessionCode]
                ?.let { queue -> synchronized(queue) { queue.toList() } }
                ?: emptyList()
    }
    
    fun getParticipantRank(sessionCode: String, viewerId: Long): Int? {
        val sortedOrders = getSortedParticipantOrders(sessionCode)
        val index = sortedOrders.indexOfFirst { it.viewerId == viewerId }
        return if (index != -1) index + 1 else null
    }
    
    fun removeParticipantOrderAndGetRank(sessionCode: String, viewerId: Long): Int? {
        val queue = participantOrderMap[sessionCode] ?: return null
        return synchronized(queue) {
            val sortedList = queue.toList()
            val index = sortedList.indexOfFirst { it.viewerId == viewerId }
            if (index != -1) {
                queue.removeIf { it.viewerId == viewerId }
                index + 1
            } else {
                null
            }
        }
    }
    
    fun removeParticipantOrderQueue(sessionCode: String) {
        participantOrderMap.remove(sessionCode)
    }
    
    fun removeParticipantOrder(sessionCode: String, viewerId: Long): Boolean {
        val queue = participantOrderMap[sessionCode] ?: return false
        return synchronized(queue) { queue.removeIf { it.viewerId == viewerId } }
    }
    
    fun rotateFirstNOrdersToNextCycle(session: ContentsSession) {
        val queue = participantOrderMap[session.sessionCode] ?: return
        synchronized(queue) {
            val iterator = queue.iterator()
            var remaining = session.maxGroupParticipants
            val updatedOrders = mutableListOf<ParticipantOrder>()
            
            while (iterator.hasNext() && remaining > 0) {
                val oldOrder = iterator.next()
                updatedOrders.add(oldOrder.nextCycle())
                remaining--
            }
            
            updatedOrders.forEach { updatedOrder ->
                queue.removeIf { it.viewerId == updatedOrder.viewerId }
                queue.add(updatedOrder)
            }
        }
    }
    
    private fun update(sessionCode: String, participantOrder: ParticipantOrder) {
        val queue = participantOrderMap.computeIfAbsent(sessionCode) { ConcurrentSkipListSet() }
        synchronized(queue) {
            queue.removeIf { it.viewerId == participantOrder.viewerId }
            queue.add(participantOrder)
        }
    }
    
}