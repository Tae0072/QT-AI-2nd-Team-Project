package com.qtai.domain.ai.api;

import com.qtai.domain.ai.api.dto.AdminAiBatchRunLogListResponse;
import com.qtai.domain.ai.api.dto.ListAdminAiBatchRunLogsQuery;

public interface ListAdminAiBatchRunLogsUseCase {

    AdminAiBatchRunLogListResponse listAdminAiBatchRunLogs(ListAdminAiBatchRunLogsQuery query);
}
