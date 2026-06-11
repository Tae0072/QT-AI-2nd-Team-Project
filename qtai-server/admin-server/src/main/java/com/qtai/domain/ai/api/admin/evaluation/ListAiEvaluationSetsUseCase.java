package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetListResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.ListAiEvaluationSetsQuery;

public interface ListAiEvaluationSetsUseCase {

    AiEvaluationSetListResponse listEvaluationSets(ListAiEvaluationSetsQuery query);
}
