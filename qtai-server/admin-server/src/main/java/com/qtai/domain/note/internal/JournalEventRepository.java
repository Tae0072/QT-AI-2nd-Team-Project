package com.qtai.domain.note.internal;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JournalEventRepository extends JpaRepository<JournalEvent, Long> {

    boolean existsByEventId(UUID eventId);

    Optional<JournalEvent> findByEventId(UUID eventId);

    List<JournalEvent> findByStatusOrderByOccurredAtAsc(JournalEventStatus status);

    /**
     * 재처리 대상(due) 이벤트 id를 오래된 순으로 조회한다(P1-10 재처리기).
     *
     * <p>대상: 아직 처리되지 않은 PENDING 전부 + 재시도 상한 미만이며 백오프가 도래한 FAILED.
     * {@code nextAttemptAt}이 NULL이면 즉시 대상으로 본다. PROCESSED와 상한 도달 FAILED(=dead-letter)는 제외.
     */
    @Query("""
            SELECT j.id FROM JournalEvent j
            WHERE j.deletedAt IS NULL
              AND (
                    j.status = com.qtai.domain.note.internal.JournalEventStatus.PENDING
                 OR (
                        j.status = com.qtai.domain.note.internal.JournalEventStatus.FAILED
                    AND j.retryCount < :maxRetry
                    AND (j.nextAttemptAt IS NULL OR j.nextAttemptAt <= :now)
                    )
              )
            ORDER BY j.occurredAt ASC, j.id ASC
            """)
    List<Long> findDueForReprocess(@Param("now") LocalDateTime now,
                                   @Param("maxRetry") int maxRetry,
                                   Pageable pageable);
}
