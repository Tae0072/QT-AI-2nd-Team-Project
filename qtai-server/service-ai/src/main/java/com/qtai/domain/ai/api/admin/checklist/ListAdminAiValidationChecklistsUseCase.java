package com.qtai.domain.ai.api.admin.checklist;

import com.qtai.domain.ai.api.admin.checklist.dto.AdminAiValidationChecklistListResponse;
import com.qtai.domain.ai.api.admin.checklist.dto.ListAdminAiValidationChecklistsQuery;

public interface ListAdminAiValidationChecklistsUseCase {

    AdminAiValidationChecklistListResponse listAdminAiValidationChecklists(
            ListAdminAiValidationChecklistsQuery query
    );
}
