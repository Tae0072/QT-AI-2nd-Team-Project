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
import com.qtai.domain.ai.api.admin.monitoring.GetAdminAiMonitoringUseCase;
import com.qtai.domain.ai.api.admin.monitoring.dto.AdminAiMonitoringResponse;
import com.qtai.domain.ai.api.admin.monitoring.dto.GetAdminAiMonitoringQuery;

@RestController
@RequestMapping("/api/v1/admin/ai/monitoring")
public class AdminAiMonitoringController {

    private final GetAdminAiMonitoringUseCase useCase;

    public AdminAiMonitoringController(GetAdminAiMonitoringUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminAiMonitoringResponse>> getMonitoring(
            Authentication authentication,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        AdminAiAuthentication adminAuthentication = AdminAiAuthentication.requireMonitoring(authentication);
        AdminAiMonitoringResponse response = useCase.getAdminAiMonitoring(new GetAdminAiMonitoringQuery(
                adminAuthentication.adminId(),
                adminAuthentication.memberRole(),
                adminAuthentication.adminRole(),
                from,
                to
        ));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return AiWebExceptionResponses.business(exception);
    }
}
