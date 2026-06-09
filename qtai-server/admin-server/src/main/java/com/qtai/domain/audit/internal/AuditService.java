package com.qtai.domain.audit.internal;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

/**
 * 감사 도메인 진입점. 체크리스트 변경 감사에 필요한 최소 write 트랜잭션을 제공한다.
 *
 * 횡단 관심사 정책: audit은 client/ 어댑터 없이 다른 도메인 Service가 직접 의존한다.
 */
@Service
public class AuditService implements WriteAuditLogUseCase {

    private final AuditRepository auditRepository;
    private final Clock clock;

    @Autowired
    public AuditService(AuditRepository auditRepository) {
        this(auditRepository, Clock.systemDefaultZone());
    }

    AuditService(AuditRepository auditRepository, Clock clock) {
        this.auditRepository = auditRepository;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void write(AuditLogWriteRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        auditRepository.save(AuditLog.create(
                request.adminUserId(),
                request.actorType(),
                request.actorId(),
                request.actorLabel(),
                request.actionType(),
                request.targetType(),
                request.targetId(),
                request.beforeJson(),
                request.afterJson(),
                OffsetDateTime.now(clock)
        ));
    }
}
