package com.qtai.common.exception;

/**
 * 비즈니스 규칙 위반을 표현하는 도메인 공통 예외.
 *
 * GlobalExceptionHandler가 이 예외를 잡아 ErrorCode 기반으로
 * ApiResponse.error(...)로 변환한다 — 도메인 코드는 HTTP를 알 필요 없다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /** 상황별 컨텍스트 메시지가 필요할 때 (예: "memberId=3 not found"). */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
