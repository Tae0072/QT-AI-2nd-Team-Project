package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.CreateValidationReferenceJobCommand;
import com.qtai.domain.ai.api.dto.ValidationReferenceJobResponse;

public interface CreateValidationReferenceJobUseCase {

    ValidationReferenceJobResponse createValidationReferenceJob(CreateValidationReferenceJobCommand command);
}
