package com.qtai.domain.ai.api.generation;

import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobCommand;
import com.qtai.domain.ai.api.generation.dto.CreateAiGenerationJobResult;

public interface CreateAiGenerationJobUseCase {

    CreateAiGenerationJobResult createAiGenerationJob(CreateAiGenerationJobCommand command);
}
