package com.qtai.domain.ai.internal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEvaluationSetRepository extends JpaRepository<AiEvaluationSet, Long> {

    boolean existsByEvalTypeAndVersion(AiEvaluationType evalType, String version);

    Page<AiEvaluationSet> findByEvalType(AiEvaluationType evalType, Pageable pageable);

    Page<AiEvaluationSet> findByTargetType(AiTargetType targetType, Pageable pageable);

    Page<AiEvaluationSet> findByStatus(AiEvaluationSetStatus status, Pageable pageable);

    Page<AiEvaluationSet> findByEvalTypeAndTargetType(
            AiEvaluationType evalType,
            AiTargetType targetType,
            Pageable pageable
    );

    Page<AiEvaluationSet> findByEvalTypeAndStatus(
            AiEvaluationType evalType,
            AiEvaluationSetStatus status,
            Pageable pageable
    );

    Page<AiEvaluationSet> findByTargetTypeAndStatus(
            AiTargetType targetType,
            AiEvaluationSetStatus status,
            Pageable pageable
    );

    Page<AiEvaluationSet> findByEvalTypeAndTargetTypeAndStatus(
            AiEvaluationType evalType,
            AiTargetType targetType,
            AiEvaluationSetStatus status,
            Pageable pageable
    );
}
