package com.qtai.domain.audit.internal;

import java.time.Clock;
import java.time.OffsetDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.domain.audit.api.ListAuditUseCase;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogFilter;
import com.qtai.domain.audit.api.dto.AuditLogResponse;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;

/**
 * 감사 도메인 진입점. 변경 감사 write와 관리자 조회(read)를 제공한다.
 *
 * 횡단 관심사 정책: audit은 client/ 어댑터 없이 다른 도메인 Service가 직접 의존한다.
 */
@Service
public class AuditService implements WriteAuditLogUseCase, ListAuditUseCase {

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

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> list(AuditLogFilter filter, Pageable pageable) {
        AuditLogFilter f = filter != null ? filter : new AuditLogFilter(null, null, null, null, null);
        return auditRepository.search(
                f.actorType(), f.actorId(), f.actionType(), f.from(), f.to(), pageable)
                .map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog log) {
        return new AuditLogResponse(
                log.getId(),
                log.getAdminUserId(),
                log.getActorType(),
                log.getActorId(),
                log.getActorLabel(),
                log.getActionType(),
                log.getTargetType(),
                log.getTargetId(),
                log.getCreatedAt());
    }
}
