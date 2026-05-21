package com.qtai.domain.ai.web;

import java.time.Clock;
import java.time.OffsetDateTime;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.RegenerateAiAssetUseCase;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetCommand;
import com.qtai.domain.ai.api.dto.RegenerateAiAssetResult;

@RestController
@RequestMapping("/api/v1/admin/ai/assets")
public class AdminAiAssetController {

    private final RegenerateAiAssetUseCase regenerateAiAssetUseCase;
    private final Clock clock;

    public AdminAiAssetController(RegenerateAiAssetUseCase regenerateAiAssetUseCase) {
        this(regenerateAiAssetUseCase, Clock.systemDefaultZone());
    }

    AdminAiAssetController(RegenerateAiAssetUseCase regenerateAiAssetUseCase, Clock clock) {
        this.regenerateAiAssetUseCase = regenerateAiAssetUseCase;
        this.clock = clock;
    }

    @PostMapping("/{assetId}/regenerate")
    public ResponseEntity<ApiResponse<RegenerateAiAssetResponse>> regenerate(
            @PathVariable Long assetId,
            @RequestHeader("X-Admin-Id") Long adminId,
            @RequestHeader("X-Member-Role") String memberRole,
            @RequestHeader("X-Admin-Role") String adminRole,
            @Valid @RequestBody RegenerateAiAssetRequest request
    ) {
        RegenerateAiAssetResult result = regenerateAiAssetUseCase.regenerateAiAsset(new RegenerateAiAssetCommand(
                adminId,
                assetId,
                memberRole,
                adminRole,
                request.reason(),
                request.promptVersionId(),
                OffsetDateTime.now(clock)
        ));

        return ResponseEntity.accepted().body(ApiResponse.success(new RegenerateAiAssetResponse(
                result.generationJobId(),
                result.status(),
                result.createdAt()
        )));
    }

    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        HttpStatus status = switch (exception.getErrorCode()) {
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case AI_ASSET_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_STATUS_TRANSITION -> HttpStatus.CONFLICT;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}
