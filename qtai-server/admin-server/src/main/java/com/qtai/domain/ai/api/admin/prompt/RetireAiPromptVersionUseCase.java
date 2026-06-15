package com.qtai.domain.ai.api.admin.prompt;

import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.ChangeAiPromptVersionStatusCommand;

public interface RetireAiPromptVersionUseCase {

    AiPromptVersionResponse retireAiPromptVersion(ChangeAiPromptVersionStatusCommand command);
}
