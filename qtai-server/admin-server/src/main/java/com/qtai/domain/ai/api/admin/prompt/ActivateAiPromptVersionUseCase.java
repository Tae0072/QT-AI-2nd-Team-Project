package com.qtai.domain.ai.api.admin.prompt;

import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.ChangeAiPromptVersionStatusCommand;

public interface ActivateAiPromptVersionUseCase {

    AiPromptVersionResponse activateAiPromptVersion(ChangeAiPromptVersionStatusCommand command);
}
