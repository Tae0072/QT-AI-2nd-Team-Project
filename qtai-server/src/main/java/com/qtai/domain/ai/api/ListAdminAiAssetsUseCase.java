package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.AdminAiAssetListResponse;
import com.qtai.domain.ai.api.dto.ListAdminAiAssetsQuery;

public interface ListAdminAiAssetsUseCase {

    AdminAiAssetListResponse listAdminAiAssets(ListAdminAiAssetsQuery query);
}
