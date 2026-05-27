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
import com.qtai.domain.ai.api.RegisterAiGeneratedAssetUseCase;
import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetCommand;
import com.qtai.domain.ai.api.dto.RegisterAiGeneratedAssetResult;

@RestController
@RequestMapping("/api/v1/system/ai/assets")
public class SystemAiAssetController {

    private static final String VALIDATING = "VALIDATING";

    private final RegisterAiGeneratedAssetUseCase registerAiGeneratedAssetUseCase;
    private final Clock clock;

    @org.springframework.beans.factory.annotation.Autowired
    public SystemAiAssetController(RegisterAiGeneratedAssetUseCase registerAiGeneratedAssetUseCase) {
        this(registerAiGeneratedAssetUseCase, Clock.systemDefaultZone());
    }

    SystemAiAssetController(RegisterAiGeneratedAssetUseCase registerAiGeneratedAssetUseCase, Clock clock) {
        this.registerAiGeneratedAssetUseCase = registerAiGeneratedAssetUseCase;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SystemAiAssetResponse>> registerAsset(
            Authentication authentication,
            @Valid @RequestBody SystemAiAssetRequest request
    ) {
        SystemAiAuthentication.requireSystemBatch(authentication);
        requireValidatingStatus(request.status());

        RegisterAiGeneratedAssetResult result = registerAiGeneratedAssetUseCase.registerAiGeneratedAsset(
                new RegisterAiGeneratedAssetCommand(
                        request.generationJobId(),
                        request.assetType(),
                        request.targetType(),
                        request.targetId(),
                        compactObjectJson(request.payloadJson(), "payloadJson"),
                        request.sourceLabel(),
                        OffsetDateTime.now(clock)
                )
        );

        return ResponseEntity.accepted().body(ApiResponse.success(new SystemAiAssetResponse(
                result.assetId(),
                result.status()
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

    private static void requireValidatingStatus(String status) {
        if (status == null || VALIDATING.equals(status)) {
            return;
        }
        throw new BusinessException(ErrorCode.INVALID_INPUT, "status must be VALIDATING when provided");
    }

    private static String compactObjectJson(JsonNode jsonNode, String fieldName) {
        if (jsonNode == null || !jsonNode.isObject()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be a JSON object");
        }
        return jsonNode.toString();
    }
}
