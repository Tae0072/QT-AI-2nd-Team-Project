package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.dto.GetAdminAiAssetQuery;

public interface GetAdminAiAssetUseCase {

    AdminAiAssetDetailResponse getAdminAiAsset(GetAdminAiAssetQuery query);
}
