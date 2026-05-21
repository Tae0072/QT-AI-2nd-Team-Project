package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetResult;

public interface RegenerateAiAssetUseCase {

    RegenerateAiAssetResult regenerateAiAsset(RegenerateAiAssetCommand command);
}
