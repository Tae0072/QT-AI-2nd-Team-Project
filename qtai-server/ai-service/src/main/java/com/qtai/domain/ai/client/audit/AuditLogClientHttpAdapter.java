package com.qtai.domain.ai.client.audit;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;
import com.qtai.domain.ai.client.http.AiClientProperties;
import com.qtai.domain.ai.client.http.AiHttpSupport;

@Component("aiAuditLogClientHttpAdapter")
@ConditionalOnProperty(name = "qtai.ai.client.mode", havingValue = "http")
@ConditionalOnMissingBean(AuditLogClient.class)
public class AuditLogClientHttpAdapter implements AuditLogClient {

    private static final String DOWNSTREAM = "audit";
    private static final String AUDIT_LOG_PATH = "/api/v1/system/audit/logs";

    private final AiHttpSupport http;

    public AuditLogClientHttpAdapter(ObjectMapper objectMapper, AiClientProperties properties) {
        this.http = new AiHttpSupport(objectMapper, properties, properties.getAudit(), DOWNSTREAM);
    }

    @Override
    public void writeAuditLog(AuditLogCommand command) {
        if (command == null) {
            throw validationFailure("audit command must not be null");
        }
        http.postVoid(AUDIT_LOG_PATH, command, http.idempotencyKey(
                "audit.write",
                command.adminUserId(),
                command.actorType(),
                command.actorId(),
                command.actorLabel(),
                command.actionType(),
                command.targetType(),
                command.targetId(),
                command.beforeJson(),
                command.afterJson()
        ));
    }

    private static AiClientException validationFailure(String message) {
        return new AiClientException(FailureCode.VALIDATION_FAILED, DOWNSTREAM, message);
    }
}
