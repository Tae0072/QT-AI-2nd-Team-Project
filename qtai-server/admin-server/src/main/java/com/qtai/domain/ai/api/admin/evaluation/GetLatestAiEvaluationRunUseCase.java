package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationRunResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetLatestAiEvaluationRunQuery;

public interface GetLatestAiEvaluationRunUseCase {

    AiEvaluationRunResponse getLatestEvaluationRun(GetLatestAiEvaluationRunQuery query);
}
