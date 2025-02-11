package com.chit.app.domain.session.domain.repository

import com.chit.app.domain.member.domain.model.QMember
import com.chit.app.domain.session.application.dto.Participant
import com.chit.app.domain.session.domain.model.ContentsSession
import com.chit.app.domain.session.domain.model.QContentsSession
import com.chit.app.domain.session.domain.model.QSessionParticipant
import com.chit.app.domain.session.domain.model.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.model.status.SessionStatus
import com.chit.app.domain.session.infrastructure.JpaContentsSessionRepository
import com.chit.app.domain.session.infrastructure.JpaSessionParticipantRepository
import com.chit.app.global.handler.EntitySaveExceptionHandler
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.NonUniqueResultException
import com.querydsl.core.types.ExpressionUtils.count
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class SessionRepository(
        private val query: JPAQueryFactory,
        private val sessionRepository: JpaContentsSessionRepository,
        private val participantRepository: JpaSessionParticipantRepository,
) {
    
    private val m: QMember = QMember.member
    private val cs: QContentsSession = QContentsSession.contentsSession
    private val sp: QSessionParticipant = QSessionParticipant.sessionParticipant
    
    fun save(session: ContentsSession): ContentsSession =
            runCatching { sessionRepository.save(session) }
                    .onFailure { EntitySaveExceptionHandler.handle(it) }
                    .getOrThrow()
    
    fun existsOpenContentsSession(liveId: Long): Boolean = query
            .selectFrom(cs)
            .where(
                cs.liveId.eq(liveId),
                cs._status.eq(SessionStatus.OPEN)
            )
            .fetchFirst() != null
    
    fun findOpenContentsSessionBy(
            sessionCode: String? = null,
            streamerId: Long? = null
    ): ContentsSession? = query
            .selectFrom(cs)
            .where(
                sessionCode?.let { cs.sessionCode.eq(it) },
                streamerId?.let { cs.streamerId.eq(it) },
                cs._status.eq(SessionStatus.OPEN)
            ).fetchOne()
    
    fun findPagedParticipantsBySessionCode(sessionCode: String, pageable: Pageable): Page<Participant> {
        val condition = BooleanBuilder()
                .apply {
                    and(cs.sessionCode.eq(sessionCode))
                    and(sp._status.ne(ParticipationStatus.REJECTED))
                }
        
        // 콘텐츠 조회
        val participants: List<Participant> = query
                .select(
                    Projections.constructor(
                        Participant::class.java,
                        sp.participantId,
                        m.channelName,
                        sp._gameNickname,
                        sp._fixedPick
                    )
                )
                .from(sp)
                .join(sp.contentsSession, cs)
                .join(m).on(m.id.eq(sp.participantId))
                .where(condition)
                .orderBy(
                    sp._fixedPick.desc(),
                    sp._status.asc(),
                    sp.id.asc()
                )
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch()
        
        // 전체 카운트 조회
        val total: Long = query
                .select(count(sp.id))
                .from(sp)
                .join(sp.contentsSession, cs)
                .where(condition)
                .fetchOne() ?: 0L
        
        return PageImpl(participants, pageable, total)
    }
    
    fun addParticipant(sessionParticipant: SessionParticipant) =
            participantRepository.save(sessionParticipant).contentsSession.addParticipant()
    
    fun findParticipantBy(participantId: Long, sessionCode: String): SessionParticipant? {
        return try {
            query
                    .select(sp)
                    .from(sp)
                    .join(sp.contentsSession, cs).fetchJoin()
                    .where(
                        sp.id.eq(participantId),
                        cs.sessionCode.eq(sessionCode),
                        cs._status.eq(SessionStatus.OPEN)
                    )
                    .fetchOne()
        } catch (_: NonUniqueResultException) {
            null
        }
    }
    
    fun findParticipantBySessionIdAndParticipantId(sessionId: Long, participantId: Long): SessionParticipant? = query
            .selectFrom(sp)
            .where(
                sp.contentsSession.id.eq(sessionId),
                sp.participantId.eq(participantId)
            )
            .fetchOne()
    
    fun findSortedParticipantsBySessionCode(sessionCode: String): List<SessionParticipant> = query
            .select(sp)
            .join(sp.contentsSession, cs).fetchJoin()
            .where(
                cs.sessionCode.eq(sessionCode),
                sp._status.ne(ParticipationStatus.REJECTED)
            )
            .orderBy(
                sp._fixedPick.desc(),
                sp._status.asc(),
                sp.id.asc()
            )
            .fetch()
    
}