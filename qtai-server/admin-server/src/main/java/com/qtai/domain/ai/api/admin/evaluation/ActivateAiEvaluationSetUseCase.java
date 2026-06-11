package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationSetStatusCommand;

public interface ActivateAiEvaluationSetUseCase {

    AiEvaluationSetResponse activateEvaluationSet(ChangeAiEvaluationSetStatusCommand command);
}
