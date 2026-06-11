package com.qtai.domain.ai.api.admin.asset;

import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.admin.asset.dto.GetAdminAiAssetQuery;

public interface GetAdminAiAssetUseCase {

    AdminAiAssetDetailResponse getAdminAiAsset(GetAdminAiAssetQuery query);
}
