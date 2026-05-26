package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.api.dto.RegisterAiValidationLogResult;

public interface RegisterAiValidationLogUseCase {

    RegisterAiValidationLogResult registerAiValidationLog(RegisterAiValidationLogCommand command);
}
