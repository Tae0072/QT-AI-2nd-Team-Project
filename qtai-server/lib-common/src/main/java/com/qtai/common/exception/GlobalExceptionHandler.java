package com.qtai.common.exception;

import com.qtai.common.dto.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Arrays;
import java.util.List;
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
        List<ApiResponse.FieldError> fields = e.getBindingResult().getFieldErrors().stream()
                .map(f -> new ApiResponse.FieldError(f.getField(), f.getDefaultMessage()))
                .toList();
        log.warn("Validation failed: {}", fields);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), "입력값 검증에 실패했습니다.", fields));
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
     * 동시 요청으로 인한 DB 제약(UNIQUE 등) 위반 — 409 Conflict.
     *
     * <p>여러 서비스가 {@code existsBy} 사전검사 후 INSERT하는 패턴이라, 더블탭·재시도 시
     * 사전검사를 통과한 두 요청이 동시에 INSERT하면 UNIQUE 위반이 발생한다. 이를 잡지 않으면
     * 도메인 의도(중복=409)와 달리 500이 누출된다. 도메인별 세부 코드(S0003 등)로 더 정교하게
     * 변환하고 싶으면 각 서비스에서 먼저 catch하고, 여기로 도달한 건은 공통 충돌로 처리한다.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        log.warn("DataIntegrityViolation(동시성 또는 제약 위반): {}", e.getMostSpecificCause().getMessage());
        ErrorCode code = ErrorCode.INVALID_STATUS_TRANSITION; // C0003 / 409
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(code.getCode(), "이미 처리되었거나 중복된 요청입니다."));
    }

    /**
     * 읽을 수 없는 요청 본문(잘못된 JSON 등) — 400. 기본 처리(500) 방지.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        log.warn("HttpMessageNotReadable: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), "요청 본문을 해석할 수 없습니다."));
    }

    /**
     * {@code @RequestParam}/{@code @PathVariable} 등의 제약 위반 — 400. 기본 처리(500) 방지.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .reduce((a, b) -> a + ", " + b)
                .orElse("유효성 검증 실패");
        log.warn("ConstraintViolation: {}", message);
        return ResponseEntity
                .badRequest()
                .body(ApiResponse.error(ErrorCode.INVALID_INPUT.getCode(), message));
    }

    /**
     * 메서드 보안({@code @PreAuthorize}) 거부 — 403. 기본 처리(500) 방지.
     * (SecurityFilterChain의 accessDeniedHandler는 필터 레벨만 담당)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("AccessDenied: {}", e.getMessage());
        ErrorCode code = ErrorCode.FORBIDDEN;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), code.getMessage()));
    }

    /**
     * 존재하지 않는 경로 — 404. (DispatcherServlet의 throw-exception-if-no-handler 설정 시)
     * 기본 처리(500) 방지.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoResource(NoResourceFoundException e) {
        log.warn("NoResourceFound: {}", e.getResourcePath());
        ErrorCode code = ErrorCode.RESOURCE_NOT_FOUND;
        return ResponseEntity
                .status(code.getHttpStatus())
                .body(ApiResponse.error(code.getCode(), code.getMessage()));
    }

    /**
     * 최후 안전망 — 위 핸들러에 매칭되지 않는 모든 예외를 500으로 처리.
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
