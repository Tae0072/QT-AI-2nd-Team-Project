package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationSetCommand;

public interface CreateAiEvaluationSetUseCase {

    AiEvaluationSetResponse createEvaluationSet(CreateAiEvaluationSetCommand command);
}
