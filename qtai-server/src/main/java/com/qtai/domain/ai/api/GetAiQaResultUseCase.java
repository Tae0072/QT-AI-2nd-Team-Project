package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.GetAiQaResultCommand;
import com.qtai.domain.ai.api.dto.GetAiQaResultResult;

public interface GetAiQaResultUseCase {

    GetAiQaResultResult getAiQaResult(GetAiQaResultCommand command);
}
