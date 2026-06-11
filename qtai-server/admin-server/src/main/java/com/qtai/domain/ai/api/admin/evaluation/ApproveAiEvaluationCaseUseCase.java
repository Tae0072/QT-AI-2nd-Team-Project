package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseStatusResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.ChangeAiEvaluationCaseStatusCommand;

public interface ApproveAiEvaluationCaseUseCase {

    AiEvaluationCaseStatusResponse approveEvaluationCase(ChangeAiEvaluationCaseStatusCommand command);
}
