package com.qtai.domain.ai.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.domain.ai.api.admin.monitoring.ListAdminAiBatchRunLogsUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiBatchRunLogListResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.ListAdminAiBatchRunLogsQuery;

@RestController
@RequestMapping("/api/v1/admin/ai/batch-run-logs")
public class AdminAiBatchRunLogController {

    private final ListAdminAiBatchRunLogsUseCase listUseCase;

    public AdminAiBatchRunLogController(ListAdminAiBatchRunLogsUseCase listUseCase) {
        this.listUseCase = listUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminAiBatchRunLogListResponse>> listBatchRunLogs(
            Authentication authentication,
            @RequestParam(required = false) String batchName,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAiAuthentication adminAuthentication = AdminAiAuthentication.requireMonitoring(authentication);
        AdminAiBatchRunLogListResponse response = listUseCase.listAdminAiBatchRunLogs(
                new ListAdminAiBatchRunLogsQuery(
                        adminAuthentication.adminId(),
                        adminAuthentication.memberRole(),
                        adminAuthentication.adminRole(),
                        batchName,
                        status,
                        from,
                        to,
                        page,
                        size
                )
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return AiWebExceptionResponses.business(exception);
    }
}
