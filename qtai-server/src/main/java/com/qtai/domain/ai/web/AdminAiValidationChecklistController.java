package com.qtai.domain.ai.web;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.ActivateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.CreateAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.ListAdminAiValidationChecklistsUseCase;
import com.qtai.domain.ai.api.RetireAdminAiValidationChecklistUseCase;
import com.qtai.domain.ai.api.dto.AdminAiValidationChecklistListResponse;
import com.qtai.domain.ai.api.dto.AdminAiValidationChecklistResponse;
import com.qtai.domain.ai.api.dto.ChangeAdminAiValidationChecklistStatusCommand;
import com.qtai.domain.ai.api.dto.CreateAdminAiValidationChecklistCommand;
import com.qtai.domain.ai.api.dto.ListAdminAiValidationChecklistsQuery;

@RestController
@RequestMapping("/api/v1/admin/ai/validation-checklists")
public class AdminAiValidationChecklistController {

    private final ListAdminAiValidationChecklistsUseCase listUseCase;
    private final CreateAdminAiValidationChecklistUseCase createUseCase;
    private final ActivateAdminAiValidationChecklistUseCase activateUseCase;
    private final RetireAdminAiValidationChecklistUseCase retireUseCase;

    public AdminAiValidationChecklistController(
            ListAdminAiValidationChecklistsUseCase listUseCase,
            CreateAdminAiValidationChecklistUseCase createUseCase,
            ActivateAdminAiValidationChecklistUseCase activateUseCase,
            RetireAdminAiValidationChecklistUseCase retireUseCase
    ) {
        this.listUseCase = listUseCase;
        this.createUseCase = createUseCase;
        this.activateUseCase = activateUseCase;
        this.retireUseCase = retireUseCase;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AdminAiValidationChecklistListResponse>> listChecklists(
            Authentication authentication,
            @RequestParam(required = false) String checklistType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAiAuthentication adminAuthentication = AdminAiAuthentication.requireReviewer(authentication);
        AdminAiValidationChecklistListResponse response = listUseCase.listAdminAiValidationChecklists(
                new ListAdminAiValidationChecklistsQuery(
                        adminAuthentication.adminId(),
                        adminAuthentication.memberRole(),
                        adminAuthentication.adminRole(),
                        checklistType,
                        status,
                        page,
                        size
                )
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminAiValidationChecklistResponse>> createChecklist(
            Authentication authentication,
            @Valid @RequestBody AdminAiValidationChecklistRequest request
    ) {
        AdminAiAuthentication adminAuthentication = AdminAiAuthentication.requireReviewer(authentication);
        String status = normalizeCreateStatus(request.status());
        AdminAiValidationChecklistResponse response = createUseCase.createAdminAiValidationChecklist(
                new CreateAdminAiValidationChecklistCommand(
                        adminAuthentication.adminId(),
                        adminAuthentication.memberRole(),
                        adminAuthentication.adminRole(),
                        request.checklistType(),
                        request.version(),
                        request.contentHash(),
                        status
                )
        );
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AdminAiValidationChecklistResponse>> activateChecklist(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AdminAiAuthentication adminAuthentication = AdminAiAuthentication.requireReviewer(authentication);
        AdminAiValidationChecklistResponse response = activateUseCase.activateAdminAiValidationChecklist(
                new ChangeAdminAiValidationChecklistStatusCommand(
                        adminAuthentication.adminId(),
                        adminAuthentication.memberRole(),
                        adminAuthentication.adminRole(),
                        id
                )
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<ApiResponse<AdminAiValidationChecklistResponse>> retireChecklist(
            @PathVariable Long id,
            Authentication authentication
    ) {
        AdminAiAuthentication adminAuthentication = AdminAiAuthentication.requireReviewer(authentication);
        AdminAiValidationChecklistResponse response = retireUseCase.retireAdminAiValidationChecklist(
                new ChangeAdminAiValidationChecklistStatusCommand(
                        adminAuthentication.adminId(),
                        adminAuthentication.memberRole(),
                        adminAuthentication.adminRole(),
                        id
                )
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return AiWebExceptionResponses.business(exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException() {
        return AiWebExceptionResponses.invalidInput();
    }

    private static String normalizeCreateStatus(String status) {
        if (status == null || status.isBlank()) {
            return "DRAFT";
        }
        if ("DRAFT".equals(status)) {
            return status;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "status must be DRAFT when provided");
    }

}
