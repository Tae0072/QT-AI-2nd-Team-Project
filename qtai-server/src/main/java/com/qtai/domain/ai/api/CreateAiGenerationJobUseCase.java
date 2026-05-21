package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.dto.CreateAiGenerationJobResult;

public interface CreateAiGenerationJobUseCase {

    CreateAiGenerationJobResult createAiGenerationJob(CreateAiGenerationJobCommand command);
}
