package com.qtai.domain.ai.client.audit;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component("aiAuditLogClientMock")
@Profile({"local", "test"})
@ConditionalOnProperty(name = "qtai.ai.client.mode", havingValue = "mock", matchIfMissing = true)
@ConditionalOnMissingBean(AuditLogClient.class)
public class AuditLogClientMock implements AuditLogClient {

    private final List<AuditLogCommand> writtenCommands = new ArrayList<>();

    @Override
    public void writeAuditLog(AuditLogCommand command) {
        writtenCommands.add(command);
    }

    public List<AuditLogCommand> writtenCommands() {
        return List.copyOf(writtenCommands);
    }
}
