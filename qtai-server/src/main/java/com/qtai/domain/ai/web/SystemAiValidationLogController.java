package com.qtai.domain.ai.web;

import java.time.Clock;
import java.time.OffsetDateTime;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.RegisterAiValidationLogUseCase;
import com.qtai.domain.ai.api.dto.RegisterAiValidationLogCommand;
import com.qtai.domain.ai.api.dto.RegisterAiValidationLogResult;

@RestController
@RequestMapping("/api/v1/system/ai/validation-logs")
public class SystemAiValidationLogController {

    private final RegisterAiValidationLogUseCase registerAiValidationLogUseCase;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public SystemAiValidationLogController(RegisterAiValidationLogUseCase registerAiValidationLogUseCase) {
        this(registerAiValidationLogUseCase, Clock.systemDefaultZone());
    }

    SystemAiValidationLogController(RegisterAiValidationLogUseCase registerAiValidationLogUseCase, Clock clock) {
        this.registerAiValidationLogUseCase = registerAiValidationLogUseCase;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemAiValidationLogResponse>> registerValidationLog(
            Authentication authentication,
            @Valid @RequestBody SystemAiValidationLogRequest request
    ) {
        SystemAiAuthentication.requireSystemBatch(authentication);

        RegisterAiValidationLogResult result = registerAiValidationLogUseCase.registerAiValidationLog(
                new RegisterAiValidationLogCommand(
                        request.aiAssetId(),
                        request.validationReferenceJobId(),
                        request.layer(),
                        request.result(),
                        request.reviewerType(),
                        request.checklistVersionId(),
                        compactObjectJson(request.checklistJson(), "checklistJson"),
                        request.errorMessage(),
                        OffsetDateTime.now(clock)
                )
        );

        return ResponseEntity.accepted().body(ApiResponse.success(new SystemAiValidationLogResponse(
                result.validationLogId(),
                result.result(),
                result.assetStatus()
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

    private static String compactObjectJson(JsonNode jsonNode, String fieldName) {
        if (jsonNode == null || !jsonNode.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be a JSON object");
        }
        return jsonNode.toString();
    }
}
