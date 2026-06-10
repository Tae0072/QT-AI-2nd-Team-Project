package com.qtai.domain.notification.internal;

import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class NoticeAuditWriter {

    private final WriteAuditLogUseCase writeAuditLogUseCase;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void write(Long adminUserId, String actionType, Long noticeId, String beforeJson, String afterJson) {
        writeAuditLogUseCase.write(new AuditLogWriteRequest(
                adminUserId,
                "ADMIN",
                adminUserId,
                "ADMIN:" + adminUserId,
                actionType,
                "NOTICE",
                noticeId,
                beforeJson,
                afterJson
        ));
    }
}
