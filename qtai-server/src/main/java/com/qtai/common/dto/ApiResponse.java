package com.qtai.common.dto;

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
        ErrorBody error) {
    public record ErrorBody(String code, String message) {
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message));
    }
}