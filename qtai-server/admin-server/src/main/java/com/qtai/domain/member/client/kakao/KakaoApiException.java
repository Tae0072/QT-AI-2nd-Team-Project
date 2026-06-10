package com.qtai.domain.member.client.kakao;

/**
 * 카카오 API 호출 실패 시 발생하는 예외.
 */
public class KakaoApiException extends RuntimeException {

    public KakaoApiException(String message) {
        super(message);
    }

    public KakaoApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
