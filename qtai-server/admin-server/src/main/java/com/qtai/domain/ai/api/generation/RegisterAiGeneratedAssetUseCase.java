package com.qtai.domain.ai.api.generation;

import com.qtai.domain.ai.api.generation.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.generation.dto.RegisterAiGeneratedAssetResult;

public interface RegisterAiGeneratedAssetUseCase {

    RegisterAiGeneratedAssetResult registerAiGeneratedAsset(RegisterAiGeneratedAssetCommand command);
}
