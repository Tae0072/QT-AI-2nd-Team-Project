package com.qtai.domain.ai.internal;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEvaluationRunRepository extends JpaRepository<AiEvaluationRun, Long> {

    Optional<AiEvaluationRun> findFirstByEvaluationSetIdOrderByCreatedAtDescIdDesc(Long evaluationSetId);

    Optional<AiEvaluationRun> findFirstByPromptVersionIdAndStatusOrderByFinishedAtDescIdDesc(
            Long promptVersionId,
            AiEvaluationRunStatus status
    );
}
