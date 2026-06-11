package com.qtai.domain.ai.api.admin.checklist;

import com.qtai.domain.ai.api.admin.checklist.dto.AdminAiValidationChecklistResponse;
import com.qtai.domain.ai.api.admin.checklist.dto.CreateAdminAiValidationChecklistCommand;

public interface CreateAdminAiValidationChecklistUseCase {

    AdminAiValidationChecklistResponse createAdminAiValidationChecklist(
            CreateAdminAiValidationChecklistCommand command
    );
}
