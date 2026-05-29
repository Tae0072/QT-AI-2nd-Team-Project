package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.AdminAiValidationChecklistResponse;
import com.qtai.domain.ai.api.dto.ChangeAdminAiValidationChecklistStatusCommand;

public interface ActivateAdminAiValidationChecklistUseCase {

    AdminAiValidationChecklistResponse activateAdminAiValidationChecklist(
            ChangeAdminAiValidationChecklistStatusCommand command
    );
}
