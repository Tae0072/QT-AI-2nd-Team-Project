package com.qtai.domain.ai.api.admin.monitoring;

import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiBatchRunLogListResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.ListAdminAiBatchRunLogsQuery;

public interface ListAdminAiBatchRunLogsUseCase {

    AdminAiBatchRunLogListResponse listAdminAiBatchRunLogs(ListAdminAiBatchRunLogsQuery query);
}
