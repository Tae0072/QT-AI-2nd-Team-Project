package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationSetResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationSetStatusCommand;

public interface RetireAiEvaluationSetUseCase {

    AiEvaluationSetResponse retireEvaluationSet(ChangeAiEvaluationSetStatusCommand command);
}
