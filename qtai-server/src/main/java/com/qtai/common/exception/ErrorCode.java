package com.qtai.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C0001", "서버 내부 오류"),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C0002", "잘못된 요청"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C0003", "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C0004", "권한이 없습니다"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "C0005", "리소스를 찾을 수 없습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
