package com.qtai.domain.ai.client.audit;

import com.qtai.domain.ai.client.AiClientException;

public interface AuditLogClient {

    void writeAuditLog(AuditLogCommand command) throws AiClientException;

    record AuditLogCommand(
            Long adminUserId,
            String actorType,
            Long actorId,
            String actorLabel,
            String actionType,
            String targetType,
            Long targetId,
            String beforeJson,
            String afterJson
    ) {
    }
}
