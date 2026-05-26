package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.ReviewAiAssetCommand;
import com.qtai.domain.ai.api.dto.ReviewAiAssetResult;

public interface ReviewAiAssetUseCase {

    ReviewAiAssetResult reviewAiAsset(ReviewAiAssetCommand command);
}
