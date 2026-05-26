package com.qtai.common.exception;

import com.qtai.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 핸들러. 모든 REST 컨트롤러 예외를 표준 {@link com.qtai.common.dto.ApiResponse}
 * 형식으로 변환한다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ErrorCode code = e.getErrorCode();
        log.warn("BusinessException: {} — {}", code.getCode(), e.getMessage());
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("유효성 검증 실패");
        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    /**
     * 최후 안전망 — 위 핸들러에 매칭되지 않는 모든 예외를 500으로 처리.
     * IOException, AccessDeniedException 등 빈번한 예외는 추후 명시적 핸들러로 분리 예정.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unhandled exception", e);
        ErrorCode code = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), code.getMessage()));
    }
}
