package com.qtai.domain.audit.web;

import org.springframework.http.ResponseEntity;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

final class AuditWebExceptionResponses {

    private AuditWebExceptionResponses() {
    }

    static ResponseEntity<ApiResponse<Void>> business(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}
