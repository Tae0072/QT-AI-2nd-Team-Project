package com.qtai.common.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import org.slf4j.MDC;

/**
 * 모든 REST 응답의 표준 envelope.
 *
 * 성공/실패와 무관하게 동일한 최상위 스키마(success/data/error)를 강제해
 * 클라이언트가 분기 처리를 단순화할 수 있도록 한다.
 *
 * @param <T> data 페이로드 타입 (실패 시 null)
 */
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorBody error,
        OffsetDateTime timestamp,
        String traceId) {
    private static final ZoneId SERVER_ZONE = ZoneId.of("Asia/Seoul");

    /**
     * 에러 본문. {@code fields}는 폼 필드별 검증 에러 배열(검증 실패가 아니면 null).
     */
    public record ErrorBody(String code, String message, List<FieldError> fields) {
    }

    /** 필드별 검증 에러 (필드명 + 사유). */
    public record FieldError(String field, String reason) {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, OffsetDateTime.now(SERVER_ZONE), currentTraceId());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return error(code, message, null);
    }

    public static <T> ApiResponse<T> error(String code, String message, List<FieldError> fields) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorBody(code, message, fields),
                OffsetDateTime.now(SERVER_ZONE),
                currentTraceId()
        );
    }

    private static String currentTraceId() {
        String traceId = MDC.get("traceId");
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }
}
