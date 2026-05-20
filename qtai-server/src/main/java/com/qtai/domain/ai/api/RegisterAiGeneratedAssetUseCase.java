package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetResult;

public interface RegisterAiGeneratedAssetUseCase {

    RegisterAiGeneratedAssetResult registerAiGeneratedAsset(RegisterAiGeneratedAssetCommand command);
}
