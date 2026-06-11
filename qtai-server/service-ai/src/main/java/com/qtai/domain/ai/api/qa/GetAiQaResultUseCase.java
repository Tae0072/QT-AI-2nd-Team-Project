package com.qtai.domain.ai.api.qa;

import com.qtai.domain.ai.api.qa.dto.GetAiQaResultCommand;
import com.qtai.domain.ai.api.qa.dto.GetAiQaResultResult;

public interface GetAiQaResultUseCase {

    GetAiQaResultResult getAiQaResult(GetAiQaResultCommand command);
}
