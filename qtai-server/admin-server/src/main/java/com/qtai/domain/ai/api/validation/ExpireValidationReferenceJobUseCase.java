package com.qtai.domain.ai.api.validation;

import com.qtai.domain.ai.api.validation.dto.ExpireValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.ValidationReferenceJobResponse;

public interface ExpireValidationReferenceJobUseCase {

    ValidationReferenceJobResponse expireValidationReferenceJob(ExpireValidationReferenceJobCommand command);
}
