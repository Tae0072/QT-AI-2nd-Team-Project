package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.RequestAiQaCommand;
import com.qtai.domain.ai.api.dto.RequestAiQaResult;

public interface RequestAiQaUseCase {

    RequestAiQaResult requestAiQa(RequestAiQaCommand command);
}
