package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.GetValidationReferenceJobQuery;
import com.qtai.domain.ai.api.dto.ValidationReferenceJobResponse;

public interface GetValidationReferenceJobUseCase {

    ValidationReferenceJobResponse getValidationReferenceJob(GetValidationReferenceJobQuery query);
}
