package com.qtai.domain.ai.internal;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AiEvaluationResultRepository extends JpaRepository<AiEvaluationResult, Long> {

    List<AiEvaluationResult> findByEvaluationRunIdOrderByIdAsc(Long evaluationRunId);
}
