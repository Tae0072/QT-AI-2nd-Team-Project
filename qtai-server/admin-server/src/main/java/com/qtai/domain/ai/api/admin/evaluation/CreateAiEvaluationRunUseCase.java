package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationRunResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationRunCommand;

public interface CreateAiEvaluationRunUseCase {

    AiEvaluationRunResponse createEvaluationRun(CreateAiEvaluationRunCommand command);
}
