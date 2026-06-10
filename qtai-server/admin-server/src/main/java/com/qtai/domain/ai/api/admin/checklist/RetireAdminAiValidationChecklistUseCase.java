package com.qtai.domain.ai.api.admin.checklist;

import com.qtai.domain.ai.api.admin.checklist.dto.AdminAiValidationChecklistResponse;
import com.qtai.domain.ai.api.admin.checklist.dto.ChangeAdminAiValidationChecklistStatusCommand;

public interface RetireAdminAiValidationChecklistUseCase {

    AdminAiValidationChecklistResponse retireAdminAiValidationChecklist(
            ChangeAdminAiValidationChecklistStatusCommand command
    );
}
