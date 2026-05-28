package com.qtai.domain.ai.web;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.domain.ai.api.CreateValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.ExpireValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.GetValidationReferenceJobUseCase;
import com.qtai.domain.ai.api.dto.CreateValidationReferenceJobCommand;
import com.qtai.domain.ai.api.dto.ExpireValidationReferenceJobCommand;
import com.qtai.domain.ai.api.dto.GetValidationReferenceJobQuery;
import com.qtai.domain.ai.api.dto.ValidationReferenceJobResponse;

@RestController
@RequestMapping("/api/v1/system/validation-reference-jobs")
public class SystemValidationReferenceJobController {

    private final CreateValidationReferenceJobUseCase createUseCase;
    private final GetValidationReferenceJobUseCase getUseCase;
    private final ExpireValidationReferenceJobUseCase expireUseCase;

    public SystemValidationReferenceJobController(
            CreateValidationReferenceJobUseCase createUseCase,
            GetValidationReferenceJobUseCase getUseCase,
            ExpireValidationReferenceJobUseCase expireUseCase
    ) {
        this.createUseCase = createUseCase;
        this.getUseCase = getUseCase;
        this.expireUseCase = expireUseCase;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ValidationReferenceJobResponse>> createValidationReferenceJob(
            Authentication authentication,
            @Valid @RequestBody SystemValidationReferenceJobRequest request
    ) {
        SystemAiAuthentication.requireSystemBatch(authentication);
        ValidationReferenceJobResponse response = createUseCase.createValidationReferenceJob(
                new CreateValidationReferenceJobCommand(
                        request.sourceName(),
                        request.sourceFileName(),
                        request.sourceFileHash(),
                        request.storageUri(),
                        request.indexStorageUri(),
                        request.expiresAt()
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<ApiResponse<ValidationReferenceJobResponse>> getValidationReferenceJob(
            Authentication authentication,
            @PathVariable Long jobId
    ) {
        SystemAiAuthentication.requireSystemBatch(authentication);
        ValidationReferenceJobResponse response =
                getUseCase.getValidationReferenceJob(new GetValidationReferenceJobQuery(jobId));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{jobId}/expire")
    public ResponseEntity<ApiResponse<ValidationReferenceJobResponse>> expireValidationReferenceJob(
            Authentication authentication,
            @PathVariable Long jobId
    ) {
        SystemAiAuthentication.requireSystemBatch(authentication);
        ValidationReferenceJobResponse response =
                expireUseCase.expireValidationReferenceJob(new ExpireValidationReferenceJobCommand(jobId));
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
}
