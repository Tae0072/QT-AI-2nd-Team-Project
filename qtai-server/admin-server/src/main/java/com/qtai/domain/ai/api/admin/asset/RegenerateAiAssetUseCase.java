package com.qtai.domain.ai.api.admin.asset;

import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.RegenerateAiAssetResult;

public interface RegenerateAiAssetUseCase {

    RegenerateAiAssetResult regenerateAiAsset(RegenerateAiAssetCommand command);
}
