package com.qtai.domain.ai.api.admin.prompt;

import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionListResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.ListAiPromptVersionsQuery;

public interface ListAiPromptVersionsUseCase {

    AiPromptVersionListResponse listAiPromptVersions(ListAiPromptVersionsQuery query);
}
