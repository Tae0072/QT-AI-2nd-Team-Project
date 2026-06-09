package com.qtai.domain.ai.api.admin.asset;

import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListResponse;
import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;

public interface ListAdminAiAssetsUseCase {

    AdminAiAssetListResponse listAdminAiAssets(ListAdminAiAssetsQuery query);
}
