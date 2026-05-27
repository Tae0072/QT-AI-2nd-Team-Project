package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.AdminAiValidationChecklistListResponse;
import com.qtai.domain.ai.api.dto.ListAdminAiValidationChecklistsQuery;

public interface ListAdminAiValidationChecklistsUseCase {

    AdminAiValidationChecklistListResponse listAdminAiValidationChecklists(
            ListAdminAiValidationChecklistsQuery query
    );
}
