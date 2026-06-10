package com.qtai.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 서비스 간 RestClient 호출 대상의 base URL 모음(회의록 2026-06-09 §3 — 단일 DB·RestClient 동기 통신).
 *
 * <p>각 호출자 도메인의 {@code client/{도메인}/...RestClientAdapter}는 이 프로퍼티에서 대상 서비스의
 * base URL을 주입받아 상대 서비스의 {@code /api/v1/**} 엔드포인트를 호출한다. 값은 환경변수로
 * 오버라이드할 수 있다(예: {@code QTAI_SERVICES_BIBLE_BASE_URL}). 기본값은 로컬 멀티모듈 구동 시
 * 각 모듈이 사용하는 포트(8081~8090)를 가리킨다.
 *
 * <p>로컬 쿠버네티스/배포 환경에서는 서비스 디스커버리 호스트명(예: {@code http://service-bible:8082})으로
 * 환경변수만 바꿔 주입한다.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "qtai.services")
public class ServiceEndpointsProperties {

    /** service-user (member·notification·mission, JWT 발급) base URL. */
    private String userBaseUrl = "http://localhost:8081";

    /** service-bible (bible·qt·study·music·praise, 읽기 콘텐츠) base URL. */
    private String bibleBaseUrl = "http://localhost:8082";

    /** service-note (note·sharing·report 제출) base URL. */
    private String noteBaseUrl = "http://localhost:8083";

    /** service-ai (사전 생성/검증·F-15 단발 Q&A) base URL. */
    private String aiBaseUrl = "http://localhost:8084";

    /** admin-server (admin·audit·report 관리) base URL. */
    private String adminBaseUrl = "http://localhost:8090";
}
