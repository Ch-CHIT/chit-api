package com.chit.app.domain.session.domain.service

import com.chit.app.domain.session.domain.model.ParticipantOrder
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

object ParticipantOrderManager {
    
    private val participantOrderMap: ConcurrentHashMap<String, ConcurrentSkipListSet<ParticipantOrder>> = ConcurrentHashMap()
    
    /**
     * sessionCode 에 해당하는 ParticipantOrder 들을 정렬된 리스트 형태로 반환
     * 내부적으로 동기화(synchronized)를 사용하여 thread-safe 하게 리스트를 생성
     *
     * @return 정렬된 ParticipantOrder 의 리스트, 없으면 emptyList 반환
     */
    fun getSortedParticipantOrders(sessionCode: String): List<ParticipantOrder> {
        return participantOrderMap[sessionCode]
                ?.let { queue -> synchronized(queue) { queue.toList() } }
                ?: emptyList()
    }
    
    /**
     * sessionCode 에 해당하는 ParticipantOrder 를 추가하거나 업데이트
     * 기존에 같은 viewerId가 존재하면 먼저 제거한 후, 새로운 ParticipantOrder 를 추가
     */
    fun addOrUpdateParticipantOrder(sessionCode: String, participant: SessionParticipant, viewerId: Long, chzzkNickname: String) {
        update(sessionCode, ParticipantOrder.of(participant, viewerId, chzzkNickname))
    }
    
    /**
     * sessionCode 에 해당하는 ParticipantOrder 목록에서 viewerId에 해당하는 항목을 삭제
     *
     * @return 삭제 성공 시 true, 존재하지 않으면 false 반환
     */
    fun removeParticipantOrder(sessionCode: String, viewerId: Long): Boolean {
        val queue = participantOrderMap[sessionCode] ?: return false
        return synchronized(queue) { queue.removeIf { it.viewerId == viewerId } }
    }
    
    /**
     * sessionCode 에 해당하는 ParticipantOrder 목록에서 viewerId에 해당하는 항목을 삭제하고,
     * 해당 항목이 정렬된 리스트에서 몇 번째(0부터 시작하는 인덱스)에 위치했었는지 반환
     *
     * @return 삭제된 항목의 인덱스(0부터 시작), 해당 항목이 없으면 null 반환
     */
    fun removeParticipantOrderAndReturnIndex(sessionCode: String, viewerId: Long): Int? {
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
    
    /**
     * 주어진 세션에 대해, 최대 그룹 참가자 수(maxGroupParticipants)만큼 정렬된 ParticipantOrder 의 항목들에 대해 cycle을 진행
     * 각 ParticipantOrder에 대해 nextCycle()를 호출하여 업데이트하며, 업데이트된 항목은 다시 저장
     */
    fun advanceCycleForFirstNParticipantOrders(session: ContentsSession) {
        val queue = participantOrderMap[session.sessionCode] ?: return
        synchronized(queue) {
            val snapshot = queue.toList()
            snapshot.take(session.maxGroupParticipants).forEach { participantOrder ->
                update(session.sessionCode, participantOrder.nextCycle())
            }
        }
    }
    
    /**
     * sessionCode 에 해당하는 ParticipantOrder 목록 전체를 삭제
     */
    fun removeParticipantOrderQueue(sessionCode: String) {
        participantOrderMap.remove(sessionCode)
    }
    
    /**
     * sessionCode에 해당하는 정렬된 ParticipantOrder 리스트에서, viewerId에 해당하는 참가자의 위치(1부터 시작하는 순위)를 반환
     * - 존재하지 않으면 null 반환
     *
     * @return 1부터 시작하는 위치, 존재하지 않으면 null
     */
    fun getParticipantOrderPosition(sessionCode: String, viewerId: Long): Int? {
        val sortedOrders = getSortedParticipantOrders(sessionCode)
        val index = sortedOrders.indexOfFirst { it.viewerId == viewerId }
        return if (index != -1) index + 1 else null
    }
    
    /**
     * sessionCode 에 해당하는 ParticipantOrder 집합이 없으면 새로 생성하고,
     * 기존에 동일 viewerId가 존재하면 제거한 후, 최신 ParticipantOrder를 추가
     */
    private fun update(sessionCode: String, participantOrder: ParticipantOrder) {
        val queue = participantOrderMap.computeIfAbsent(sessionCode) { ConcurrentSkipListSet() }
        synchronized(queue) {
            queue.removeIf { it.viewerId == participantOrder.viewerId }
            queue.add(participantOrder)
        }
    }
    
}