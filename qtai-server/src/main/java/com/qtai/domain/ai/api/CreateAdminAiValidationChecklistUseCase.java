package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.AdminAiValidationChecklistResponse;
import com.qtai.domain.ai.api.dto.CreateAdminAiValidationChecklistCommand;

public interface CreateAdminAiValidationChecklistUseCase {

    AdminAiValidationChecklistResponse createAdminAiValidationChecklist(
            CreateAdminAiValidationChecklistCommand command
    );
}
