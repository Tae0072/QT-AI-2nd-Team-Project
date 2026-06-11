package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationCaseQuery;

public interface GetAiEvaluationCaseUseCase {

    AiEvaluationCaseResponse getEvaluationCase(GetAiEvaluationCaseQuery query);
}
