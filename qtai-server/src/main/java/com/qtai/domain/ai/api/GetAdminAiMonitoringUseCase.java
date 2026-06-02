package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.AdminAiMonitoringResponse;
import com.qtai.domain.ai.api.dto.GetAdminAiMonitoringQuery;

public interface GetAdminAiMonitoringUseCase {

    AdminAiMonitoringResponse getAdminAiMonitoring(GetAdminAiMonitoringQuery query);
}
