package com.qtai.domain.ai.api.validation;

import com.qtai.domain.ai.api.validation.dto.CreateValidationReferenceJobCommand;
import com.qtai.domain.ai.api.validation.dto.ValidationReferenceJobResponse;

public interface CreateValidationReferenceJobUseCase {

    ValidationReferenceJobResponse createValidationReferenceJob(CreateValidationReferenceJobCommand command);
}
