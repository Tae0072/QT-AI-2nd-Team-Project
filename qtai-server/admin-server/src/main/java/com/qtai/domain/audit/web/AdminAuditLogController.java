package com.qtai.domain.audit.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.domain.audit.api.ListAuditUseCase;
import com.qtai.domain.audit.api.dto.AuditLogListResponse;
import com.qtai.domain.audit.api.dto.ListAuditQuery;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditLogController {

    private final ListAuditUseCase listAuditUseCase;
    private final AdminAuditAuthentication adminAuditAuthentication;

    public AdminAuditLogController(
            ListAuditUseCase listAuditUseCase,
            AdminAuditAuthentication adminAuditAuthentication
    ) {
        this.listAuditUseCase = listAuditUseCase;
        this.adminAuditAuthentication = adminAuditAuthentication;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AuditLogListResponse>> listAuditLogs(
            Authentication authentication,
            @RequestParam(required = false) String actorType,
            @RequestParam(required = false) Long actorId,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) Long targetId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAuditAuthentication.AdminAuditPrincipal adminAuthentication = adminAuditAuthentication.requireAudit(authentication);
        AuditLogListResponse response = listAuditUseCase.listAuditLogs(new ListAuditQuery(
                adminAuthentication.adminId(),
                adminAuthentication.memberRole(),
                adminAuthentication.adminRole(),
                actorType,
                actorId,
                actionType,
                targetType,
                targetId,
                from,
                to,
                page,
                size
        ));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return AuditWebExceptionResponses.business(exception);
    }
}
