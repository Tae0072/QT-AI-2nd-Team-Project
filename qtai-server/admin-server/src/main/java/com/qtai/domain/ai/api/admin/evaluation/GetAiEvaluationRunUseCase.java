package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationRunResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationRunQuery;

public interface GetAiEvaluationRunUseCase {

    AiEvaluationRunResponse getEvaluationRun(GetAiEvaluationRunQuery query);
}
