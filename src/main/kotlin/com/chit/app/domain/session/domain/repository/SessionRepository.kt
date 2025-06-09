package com.chit.app.domain.session.domain.repository

import com.chit.app.domain.member.domain.model.QMember
import com.chit.app.domain.session.domain.model.Participant
import com.chit.app.domain.session.domain.model.entity.ContentsSession
import com.chit.app.domain.session.domain.model.entity.QContentsSession
import com.chit.app.domain.session.domain.model.entity.QSessionParticipant
import com.chit.app.domain.session.domain.model.entity.SessionParticipant
import com.chit.app.domain.session.domain.model.status.ParticipationStatus
import com.chit.app.domain.session.domain.model.status.SessionStatus
import com.chit.app.domain.session.infrastructure.JpaContentsSessionRepository
import com.chit.app.domain.session.infrastructure.JpaSessionParticipantRepository
import com.chit.app.global.common.handler.EntitySaveExceptionHandler
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.ExpressionUtils.count
import com.querydsl.core.types.Projections
import com.querydsl.core.types.dsl.Expressions
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
    
    fun notExistsOpenContentsSession(liveId: Long): Boolean = query
            .selectFrom(cs)
            .where(
                cs.liveId.eq(liveId),
                cs._status.eq(SessionStatus.OPEN)
            )
            .fetchFirst() == null
    
    fun findOpenContentsSessionBy(
            sessionCode: String? = null,
            streamerId: Long? = null
    ): ContentsSession? = query
            .selectFrom(cs)
            .where(
                sessionCode?.let { cs.sessionCode.eq(it) },
                streamerId?.let { cs.streamerId.eq(it) },
                cs._status.eq(SessionStatus.OPEN)
            )
            .fetchOne()
    
    fun findPagedParticipantsBySessionCode(sessionCode: String, pageable: Pageable): Page<Participant> {
        val condition = BooleanBuilder()
                .apply {
                    and(cs.sessionCode.eq(sessionCode))
                    and(sp._status.ne(ParticipationStatus.LEFT))
                }
        
        val contents = query
                .select(
                    Projections.constructor(
                        Participant::class.java,
                        Expressions.constant(0),
                        sp._round,
                        sp._fixedPick,
                        sp._status,
                        sp.viewerId,
                        sp.id,
                        m.channelName,
                        sp._gameNickname,
                    )
                )
                .from(sp)
                .join(sp.contentsSession, cs)
                .join(m).on(m.id.eq(sp.viewerId))
                .where(condition)
                .orderBy(
                    sp._round.asc(),
                    sp._fixedPick.desc(),
                    sp.id.asc()
                )
                .offset(pageable.offset)
                .limit(pageable.pageSize.toLong())
                .fetch().mapIndexed { index, event ->
                    event.copy(order = index + 1 + pageable.offset.toInt())
                }
        
        val total = query
                .select(count(sp.id))
                .from(sp)
                .join(sp.contentsSession, cs)
                .where(condition)
                .fetchOne() ?: 0L
        
        return PageImpl(contents, pageable, total)
    }
    
    fun addParticipant(sessionParticipant: SessionParticipant): SessionParticipant =
            participantRepository.save(sessionParticipant)
    
    fun findParticipantBy(
            viewerId: Long,
            sessionCode: String? = null,
            sessionId: Long? = null,
            streamerId: Long? = null
    ): SessionParticipant? = query
            .select(sp)
            .from(sp)
            .join(sp.contentsSession, cs).fetchJoin()
            .where(
                sessionCode?.let { cs.sessionCode.eq(it) },
                sessionId?.let { cs.id.eq(it) },
                streamerId?.let { cs.streamerId.eq(it) },
                cs._status.eq(SessionStatus.OPEN),
                sp.viewerId.eq(viewerId),
                sp._status.eq(ParticipationStatus.JOINED)
            )
            .fetchOne()
    
    fun findGameParticipationCodeBy(sessionCode: String, viewerId: Long): String? = query
            .select(cs._gameParticipationCode)
            .from(sp)
            .join(sp.contentsSession, cs)
            .where(
                cs.sessionCode.eq(sessionCode),
                sp.viewerId.eq(viewerId)
            )
            .fetchOne()
    
    fun setAllParticipantsToLeft(sessionCode: String) = query
            .update(sp)
            .set(sp._status, ParticipationStatus.LEFT)
            .where(sp.contentsSession.sessionCode.eq(sessionCode))
            .execute()
    
    fun existsParticipantInSession(sessionId: Long, viewerId: Long): Boolean = query
            .selectOne()
            .from(sp)
            .join(sp.contentsSession, cs)
            .where(
                cs.id.eq(sessionId),
                sp.viewerId.eq(viewerId),
                sp._status.eq(ParticipationStatus.JOINED)
            )
            .fetchFirst() != null
    
    fun findFirstPartyParticipants(sessionId: Long, maxGroupParticipants: Int): List<SessionParticipant> = query
            .selectFrom(sp)
            .where(sp.contentsSession.id.eq(sessionId))
            .orderBy(
                sp._round.asc(),
                sp._fixedPick.desc(),
                sp.id.asc()
            )
            .limit(maxGroupParticipants.toLong())
            .fetch()
    
    fun existsOpenSessionByChannelId(channelId: String): Boolean = query
            .selectFrom(cs)
            .join(m).on(cs.streamerId.eq(m.id))
            .where(
                m.channelId.eq(channelId),
                cs._status.eq(SessionStatus.OPEN)
            )
            .fetchFirst() != null
    
}