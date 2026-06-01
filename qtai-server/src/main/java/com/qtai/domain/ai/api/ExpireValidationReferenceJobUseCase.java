package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.ExpireValidationReferenceJobCommand;
import com.qtai.domain.ai.api.dto.ValidationReferenceJobResponse;

public interface ExpireValidationReferenceJobUseCase {

    ValidationReferenceJobResponse expireValidationReferenceJob(ExpireValidationReferenceJobCommand command);
}
