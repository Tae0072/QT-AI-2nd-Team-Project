package com.qtai.domain.ai.api.admin.asset;

import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetResult;

public interface ReviewAiAssetUseCase {

    ReviewAiAssetResult reviewAiAsset(ReviewAiAssetCommand command);
}
