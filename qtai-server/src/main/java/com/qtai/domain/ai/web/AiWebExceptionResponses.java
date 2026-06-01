package com.qtai.domain.ai.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

final class AiWebExceptionResponses {

    private AiWebExceptionResponses() {
    }

    static ResponseEntity<ApiResponse<Void>> business(BusinessException exception) {
        HttpStatus status = switch (exception.getErrorCode()) {
            case UNAUTHORIZED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case INVALID_STATUS_TRANSITION -> HttpStatus.CONFLICT;
            case AI_GENERATION_JOB_NOT_FOUND, AI_ASSET_NOT_FOUND, CHECKLIST_NOT_FOUND,
                    VALIDATION_REFERENCE_JOB_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE_CHECKLIST_VERSION -> HttpStatus.CONFLICT;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    static ResponseEntity<ApiResponse<Void>> invalidInput() {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}
