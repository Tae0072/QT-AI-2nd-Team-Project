package com.qtai.domain.ai.api.admin.prompt;

import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.GetAiPromptVersionQuery;

public interface GetAiPromptVersionUseCase {

    AiPromptVersionResponse getAiPromptVersion(GetAiPromptVersionQuery query);
}
