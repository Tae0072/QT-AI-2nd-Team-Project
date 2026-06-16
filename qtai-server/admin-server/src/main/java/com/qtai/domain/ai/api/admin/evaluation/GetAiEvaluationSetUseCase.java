package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.GetAiEvaluationSetQuery;

public interface GetAiEvaluationSetUseCase {

    AiEvaluationSetResponse getEvaluationSet(GetAiEvaluationSetQuery query);
}
