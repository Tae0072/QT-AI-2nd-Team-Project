package com.qtai.common.dto;

import java.time.OffsetDateTime;
import java.time.ZoneId;
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

    public record ErrorBody(String code, String message) {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null, OffsetDateTime.now(SERVER_ZONE), currentTraceId());
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(
                false,
                null,
                new ErrorBody(code, message),
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
