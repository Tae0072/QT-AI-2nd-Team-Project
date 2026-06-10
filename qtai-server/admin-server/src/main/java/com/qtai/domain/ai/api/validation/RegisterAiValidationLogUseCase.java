package com.qtai.domain.ai.api.validation;

import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.api.validation.dto.RegisterAiValidationLogResult;

public interface RegisterAiValidationLogUseCase {

    RegisterAiValidationLogResult registerAiValidationLog(RegisterAiValidationLogCommand command);
}
