package com.qtai.domain.audit.api;

import com.qtai.domain.audit.api.dto.AuditLogListResponse;
import com.qtai.domain.audit.api.dto.ListAuditQuery;

public interface ListAuditUseCase {

    AuditLogListResponse listAuditLogs(ListAuditQuery query);
}
