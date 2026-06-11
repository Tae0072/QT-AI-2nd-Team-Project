package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationCaseCommand;

public interface CreateAiEvaluationCaseUseCase {

    AiEvaluationCaseResponse createEvaluationCase(CreateAiEvaluationCaseCommand command);
}
