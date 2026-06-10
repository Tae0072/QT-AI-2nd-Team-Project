package com.qtai.domain.audit.internal;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.domain.audit.api.ListAdminDashboardAuditLogsUseCase;
import com.qtai.domain.audit.api.dto.AdminDashboardAuditLog;

/**
 * 관리자 대시보드용 최근 감사 로그 조회.
 *
 * <p>beforeJson/afterJson 같은 원문 snapshot은 대시보드에 노출하지 않는다.
 */
@Service
class AdminDashboardAuditLogService implements ListAdminDashboardAuditLogsUseCase {

    private static final int DEFAULT_SIZE = 5;
    private static final int MAX_SIZE = 20;

    private final AuditQueryRepository repository;

    AdminDashboardAuditLogService(AuditQueryRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDashboardAuditLog> listRecentAuditLogs(int size) {
        int resolvedSize = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_SIZE);
        return repository.findRecent(PageRequest.of(
                        0,
                        resolvedSize,
                        Sort.by(Sort.Direction.DESC, "createdAt", "id")))
                .stream()
                .map(AdminDashboardAuditLogService::toResponse)
                .toList();
    }

    private static AdminDashboardAuditLog toResponse(AuditQueryRepository.DashboardAuditLogRow row) {
        return new AdminDashboardAuditLog(
                row.id(),
                row.adminUserId(),
                row.actorType(),
                row.actionType(),
                row.targetType(),
                row.targetId(),
                row.createdAt()
        );
    }
}
