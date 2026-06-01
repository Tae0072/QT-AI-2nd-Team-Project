package com.qtai.common.exception;

import com.qtai.common.dto.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Arrays;
import java.util.stream.Collectors;

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
     * 쿼리 파라미터·경로 변수의 타입 변환 실패 처리.
     * 대표 사례: {@code ?category=INVALID} 같이 enum에 정의 안 된 값.
     * Exception 최후 안전망(500)으로 빠지지 않도록 명시적으로 400 응답.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        String paramName = e.getName();
        Object value = e.getValue();
        // value가 null이면 "null이(가) 유효하지 않습니다"라는 어색한 메시지가 나가므로 "(누락)" 으로 치환
        String valueStr = value == null ? "(누락)" : value.toString();
        Class<?> requiredType = e.getRequiredType();

        String message;
        if (requiredType != null && requiredType.isEnum()) {
            String allowed = Arrays.stream(requiredType.getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.joining(", "));
            message = String.format("파라미터 '%s'의 값 '%s'이(가) 유효하지 않습니다. 허용 값: %s",
                    paramName, valueStr, allowed);
        } else {
            String typeName = requiredType != null ? requiredType.getSimpleName() : "unknown";
            message = String.format("파라미터 '%s'의 값 '%s'이(가) %s 형식이 아닙니다.",
                    paramName, valueStr, typeName);
        }
        log.warn("Type mismatch: {}", message);
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
