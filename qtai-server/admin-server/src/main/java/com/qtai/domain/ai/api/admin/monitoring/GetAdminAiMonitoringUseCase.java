package com.qtai.domain.ai.api.admin.monitoring;

import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiMonitoringResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.GetAdminAiMonitoringQuery;

public interface GetAdminAiMonitoringUseCase {

    AdminAiMonitoringResponse getAdminAiMonitoring(GetAdminAiMonitoringQuery query);
}
