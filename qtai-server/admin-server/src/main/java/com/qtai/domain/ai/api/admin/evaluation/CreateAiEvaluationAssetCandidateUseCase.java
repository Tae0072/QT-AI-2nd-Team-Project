package com.qtai.domain.ai.api.admin.evaluation;

import com.qtai.domain.ai.api.admin.evaluation.dto.AiEvaluationCaseResponse;
import com.qtai.domain.ai.api.admin.evaluation.dto.CreateAiEvaluationAssetCandidateCommand;

public interface CreateAiEvaluationAssetCandidateUseCase {

    AiEvaluationCaseResponse createAssetCandidate(CreateAiEvaluationAssetCandidateCommand command);
}
