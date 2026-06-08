package com.qtai.common.exception;

import lombok.Getter;

/**
 * 비즈니스 규칙 위반 시 던지는 런타임 예외. {@link GlobalExceptionHandler}가 잡아
 * {@link ErrorCode} 기반으로 HTTP 응답을 변환한다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage);
        this.errorCode = errorCode;
    }
}
