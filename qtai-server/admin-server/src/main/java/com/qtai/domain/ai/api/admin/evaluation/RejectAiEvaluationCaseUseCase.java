package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseStatusResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationCaseStatusCommand;

public interface RejectAiEvaluationCaseUseCase {

    AiEvaluationCaseStatusResponse rejectEvaluationCase(ChangeAiEvaluationCaseStatusCommand command);
}
