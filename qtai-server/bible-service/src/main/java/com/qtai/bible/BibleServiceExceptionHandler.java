package com.qtai.bible;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * bible-service 공통 예외 처리(inbound와 함께 등록). 도메인 {@link BusinessException}을 ErrorCode 상태·
 * 표준 envelope로 변환해 모든 컨트롤러 엔드포인트에 일반화한다(엔드포인트별 try/catch 제거, 500 누출 방지).
 *
 * <p>bible-service는 Spring Security를 쓰지 않으므로(인증은 {@link GatewayHeaderAuthenticationFilter}),
 * servlet 공통 {@code lib-common-web.GlobalExceptionHandler}(starter-security 전이) 대신 경량 advice를 둔다.
 * 로그/응답에 민감 정보·token 값을 남기지 않는다(CLAUDE.md §9).
 */
@RestControllerAdvice
public class BibleServiceExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }
}
