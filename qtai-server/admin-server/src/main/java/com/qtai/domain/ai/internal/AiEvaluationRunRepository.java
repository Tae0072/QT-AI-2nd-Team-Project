package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface AiEvaluationRunRepository extends JpaRepository<AiEvaluationRun, Long> {

    Optional<AiEvaluationRun> findFirstByEvaluationSetIdOrderByCreatedAtDescIdDesc(Long evaluationSetId);

    Optional<AiEvaluationRun> findFirstByPromptVersionIdAndStatusOrderByFinishedAtDescIdDesc(
            Long promptVersionId,
            AiEvaluationRunStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AiEvaluationRun> findByIdAndStatus(Long id, AiEvaluationRunStatus status);

    @Query("""
            select run.id
            from AiEvaluationRun run
            where run.status = com.qtai.domain.ai.internal.AiEvaluationRunStatus.RUNNING
              and run.startedAt < :threshold
            order by run.startedAt asc, run.id asc
            """)
    List<Long> findStaleRunningRunIds(OffsetDateTime threshold, Pageable pageable);
}
