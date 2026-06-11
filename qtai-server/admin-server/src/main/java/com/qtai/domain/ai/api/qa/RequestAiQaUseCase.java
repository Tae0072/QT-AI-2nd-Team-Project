package com.qtai.domain.ai.api.qa;

import com.qtai.domain.ai.api.qa.dto.RequestAiQaCommand;
import com.qtai.domain.ai.api.qa.dto.RequestAiQaResult;

public interface RequestAiQaUseCase {

    RequestAiQaResult requestAiQa(RequestAiQaCommand command);
}
