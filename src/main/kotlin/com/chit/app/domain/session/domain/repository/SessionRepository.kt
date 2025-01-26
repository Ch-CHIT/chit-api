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
import com.chit.app.global.delegate.logger
import com.chit.app.global.handler.EntitySaveExceptionHandler
import com.querydsl.core.BooleanBuilder
import com.querydsl.core.types.ExpressionUtils.count
import com.querydsl.core.types.Projections
import com.querydsl.jpa.impl.JPAQueryFactory
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorService

@Repository
class SessionRepository(
        private val query: JPAQueryFactory,
        private val executor: ExecutorService,
        private val sessionRepository: JpaContentsSessionRepository,
        private val participantRepository: JpaSessionParticipantRepository,
) {
    
    private val log = logger<SessionRepository>()
    
    private val m: QMember = QMember.member
    private val cs: QContentsSession = QContentsSession.contentsSession
    private val sp: QSessionParticipant = QSessionParticipant.sessionParticipant
    
    fun save(session: ContentsSession): ContentsSession =
            runCatching { sessionRepository.save(session) }
                    .onFailure { EntitySaveExceptionHandler.handle(it) }
                    .getOrThrow()
    
    fun hasOpenContentsSession(liveId: Long?): Boolean = query
            .selectOne()
            .from(cs)
            .where(
                liveId?.let { cs.id.eq(it) },
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
        
        try {
            val contentFuture: CompletableFuture<List<Participant>> = CompletableFuture.supplyAsync({
                query
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
            }, executor)
            
            val totalFuture: CompletableFuture<Long> = CompletableFuture.supplyAsync({
                query
                        .select(count(sp.id))
                        .from(sp)
                        .where(condition)
                        .fetchOne() ?: 0L
            }, executor)
            
            CompletableFuture.allOf(contentFuture, totalFuture).join()
            return PageImpl(contentFuture.get(), pageable, totalFuture.get())
        } catch (e: ExecutionException) {
            log.error("데이터 쿼리 실행 중 오류 발생 - sessionCode: $sessionCode, pageable: $pageable", e)
            throw IllegalStateException("일시적인 문제가 발생했습니다. 잠시 후 다시 시도해주세요.")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            log.error("데이터 쿼리 실행이 중단됨 - sessionCode: $sessionCode, pageable: $pageable", e)
            throw IllegalStateException("서비스가 정상적으로 처리되지 못했습니다. 다시 시도해주세요.")
        }
    }
    
    fun addParticipant(sessionParticipant: SessionParticipant) =
            participantRepository.save(sessionParticipant).contentsSession.addParticipant()
    
    fun findParticipantBySessionIdAndParticipantId(sessionId: Long, participantId: Long): SessionParticipant? = query
            .selectFrom(sp)
            .where(
                sp.contentsSession.id.eq(sessionId),
                sp.participantId.eq(participantId)
            )
            .fetchOne()
    
    fun findSortedParticipantsBySessionCode(sessionCode: String): List<SessionParticipant> =
            participantRepository.findSortedParticipantsBySessionCode(sessionCode, ParticipationStatus.REJECTED)
    
}