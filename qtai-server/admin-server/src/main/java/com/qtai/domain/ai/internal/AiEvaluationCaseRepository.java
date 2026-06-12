package com.qtai.domain.ai.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEvaluationCaseRepository extends JpaRepository<AiEvaluationCase, Long> {

    long countByEvaluationSetIdAndStatus(Long evaluationSetId, AiEvaluationCaseStatus status);

    Page<AiEvaluationCase> findByEvaluationSetId(Long evaluationSetId, Pageable pageable);

    Page<AiEvaluationCase> findByEvaluationSetIdAndStatus(
            Long evaluationSetId,
            AiEvaluationCaseStatus status,
            Pageable pageable
    );

    boolean existsBySourceTypeAndSourceId(AiEvaluationSourceType sourceType, Long sourceId);
}
