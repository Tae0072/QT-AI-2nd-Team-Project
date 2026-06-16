package com.qtai.domain.ai.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

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
import com.qtai.domain.ai.api.admin.prompt.ActivateAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.CreateAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.GetAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.ListAiPromptVersionsUseCase;
import com.qtai.domain.ai.api.admin.prompt.RetireAiPromptVersionUseCase;
import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionListResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.AiPromptVersionResponse;
import com.qtai.domain.ai.api.admin.prompt.dto.ChangeAiPromptVersionStatusCommand;
import com.qtai.domain.ai.api.admin.prompt.dto.CreateAiPromptVersionCommand;
import com.qtai.domain.ai.api.admin.prompt.dto.GetAiPromptVersionQuery;
import com.qtai.domain.ai.api.admin.prompt.dto.ListAiPromptVersionsQuery;

@RestController
@RequestMapping("/api/v1/admin/ai/prompt-versions")
public class AdminAiPromptVersionController {

    private final ListAiPromptVersionsUseCase listUseCase;
    private final GetAiPromptVersionUseCase getUseCase;
    private final CreateAiPromptVersionUseCase createUseCase;
    private final ActivateAiPromptVersionUseCase activateUseCase;
    private final RetireAiPromptVersionUseCase retireUseCase;
    private final AdminAiAuthentication adminAiAuthentication;

    public AdminAiPromptVersionController(
            ListAiPromptVersionsUseCase listUseCase,
            GetAiPromptVersionUseCase getUseCase,
            CreateAiPromptVersionUseCase createUseCase,
            ActivateAiPromptVersionUseCase activateUseCase,
            RetireAiPromptVersionUseCase retireUseCase,
            AdminAiAuthentication adminAiAuthentication
    ) {
        this.listUseCase = listUseCase;
        this.getUseCase = getUseCase;
        this.createUseCase = createUseCase;
        this.activateUseCase = activateUseCase;
        this.retireUseCase = retireUseCase;
        this.adminAiAuthentication = adminAiAuthentication;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<AiPromptVersionListResponse>> listPromptVersions(
            Authentication authentication,
            @RequestParam(required = false) String promptType,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireReviewer(authentication);
        return ResponseEntity.ok(ApiResponse.success(listUseCase.listAiPromptVersions(new ListAiPromptVersionsQuery(
                principal.adminId(),
                principal.memberRole(),
                principal.adminRole(),
                promptType,
                status,
                page,
                size
        ))));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AiPromptVersionResponse>> getPromptVersion(
            Authentication authentication,
            @PathVariable Long id
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireReviewer(authentication);
        return ResponseEntity.ok(ApiResponse.success(getUseCase.getAiPromptVersion(new GetAiPromptVersionQuery(
                principal.adminId(),
                principal.memberRole(),
                principal.adminRole(),
                id
        ))));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AiPromptVersionResponse>> createPromptVersion(
            Authentication authentication,
            @Valid @RequestBody AiPromptVersionRequest request
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireReviewer(authentication);
        AiPromptVersionResponse response = createUseCase.createAiPromptVersion(new CreateAiPromptVersionCommand(
                principal.adminId(),
                principal.memberRole(),
                principal.adminRole(),
                request.promptType(),
                request.version(),
                request.systemPrompt(),
                request.userPromptTemplate(),
                request.modelName(),
                request.temperature(),
                request.maxTokens(),
                request.description()
        ));
        return ResponseEntity.status(201).body(ApiResponse.success(response));
    }

    @PostMapping("/{id}/activate")
    public ResponseEntity<ApiResponse<AiPromptVersionResponse>> activatePromptVersion(
            Authentication authentication,
            @PathVariable Long id
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireReviewer(authentication);
        return ResponseEntity.ok(ApiResponse.success(activateUseCase.activateAiPromptVersion(
                new ChangeAiPromptVersionStatusCommand(
                        principal.adminId(),
                        principal.memberRole(),
                        principal.adminRole(),
                        id
                )
        )));
    }

    @PostMapping("/{id}/retire")
    public ResponseEntity<ApiResponse<AiPromptVersionResponse>> retirePromptVersion(
            Authentication authentication,
            @PathVariable Long id
    ) {
        AdminAiAuthentication.AdminAiPrincipal principal = adminAiAuthentication.requireReviewer(authentication);
        return ResponseEntity.ok(ApiResponse.success(retireUseCase.retireAiPromptVersion(
                new ChangeAiPromptVersionStatusCommand(
                        principal.adminId(),
                        principal.memberRole(),
                        principal.adminRole(),
                        id
                )
        )));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        return AiWebExceptionResponses.business(exception);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValidException() {
        return AiWebExceptionResponses.invalidInput();
    }

    public record AiPromptVersionRequest(
            @NotBlank String promptType,
            @NotBlank String version,
            String systemPrompt,
            @NotBlank String userPromptTemplate,
            String modelName,
            @DecimalMin("0.0") @DecimalMax("2.0") Double temperature,
            @Min(1) @Max(20000) Integer maxTokens,
            String description
    ) {
    }
}
