package com.qtai.common.security;

import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 서비스 간 RestClient 호출 시, 현재 사용자 요청에 실린 {@code Authorization} 헤더를 대상 서비스로 그대로 전달한다.
 *
 * <p>설계 §5/§81(회의록 2026-06-09): JWT는 유저 서비스가 발급하고 각 서비스가 공유키로 필터 검증한다.
 * 따라서 사용자 요청 맥락에서 일어나는 서비스 간 조회는 별도 토큰 발급 없이 들어온 JWT를 전달하면 된다
 * (유저 서비스 재호출 없음). 호출자 도메인의 {@code client/{도메인}/...RestClientAdapter}가
 * {@code restClient...headers(ServiceCallAuthForwarder::forward)} 형태로 사용한다.
 *
 * <p>주의: 사용자 요청 컨텍스트가 없는 배치/스케줄러(SYSTEM_BATCH) 호출에는 전달할 사용자 JWT가 없다.
 * 그런 호출의 서비스 간 인증은 별도 시스템 토큰 메커니즘이 필요하며 이 유틸의 책임 범위가 아니다.
 */
public final class ServiceCallAuthForwarder {

    private ServiceCallAuthForwarder() {
    }

    /** 현재 요청의 Authorization 헤더가 있으면 호출 헤더에 복사한다. */
    public static void forward(HttpHeaders headers) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            String authorization = servletAttributes.getRequest().getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && !authorization.isBlank()) {
                headers.set(HttpHeaders.AUTHORIZATION, authorization);
            }
        }
    }
}
