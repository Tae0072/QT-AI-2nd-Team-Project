package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseListResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.ListAiEvaluationCasesQuery;

public interface ListAiEvaluationCasesUseCase {

    AiEvaluationCaseListResponse listEvaluationCases(ListAiEvaluationCasesQuery query);
}
