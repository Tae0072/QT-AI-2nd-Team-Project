package com.qtai.domain.ai.api.admin.prompt;

import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.CreateAiPromptVersionCommand;

public interface CreateAiPromptVersionUseCase {

    AiPromptVersionResponse createAiPromptVersion(CreateAiPromptVersionCommand command);
}
