package com.qtai.domain.ai.api.validation;

import com.qtai.domain.ai.api.validation.dto.GetValidationReferenceJobQuery;
import com.qtai.domain.ai.api.validation.dto.ValidationReferenceJobResponse;

public interface GetValidationReferenceJobUseCase {

    ValidationReferenceJobResponse getValidationReferenceJob(GetValidationReferenceJobQuery query);
}
