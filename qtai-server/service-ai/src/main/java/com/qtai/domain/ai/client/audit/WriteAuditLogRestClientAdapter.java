package com.qtai.domain.ai.client.audit;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.audit.api.WriteAuditLogUseCase;
import com.qtai.domain.audit.api.dto.AuditLogWriteRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * audit 도메인 {@link WriteAuditLogUseCase}의 service-ai 구현 — admin-server(audit 소유) HTTP 호출 어댑터.
 *
 * <p>AI 검수·생성 작업의 감사 기록을 admin-server에 동기 기록한다. audit은 admin-server 소관이라 service-ai는 api
 * 계약만 가져와 client 어댑터로 구현한다(CLAUDE.md §4). 이 어댑터가 기존 {@code WriteAuditLogUseCaseMock}(no-op)을 대체한다.
 *
 * <p>인증(배치 경로): 사용자 JWT가 없으므로 {@link SystemTokenProvider}로 단명 SYSTEM_BATCH 토큰을 발급해 Bearer로
 * 호출한다. 수신({@code POST /api/v1/system/audit-logs})은 admin-server의 {@code /api/v1/system/**} = SYSTEM_BATCH 전용이며,
 * admin-server가 시스템 토큰 폴백으로 검증한다. 시크릿 미설정 부팅을 위해 {@link ObjectProvider}로 주입한다(운영 생성자 @Autowired).
 *
 * <p><b>fire-and-forget</b>(WriteAuditLogUseCase javadoc): 감사 기록은 횡단 관심사라 전송 실패가 AI 작업 자체를 깨지
 * 않도록 한다. 광범위 catch 대신 {@link RestClientException}만 잡아 경고만 남기고 진행한다(§9). 토큰·시크릿·민감 본문은
 * 로그에 남기지 않는다(§7·§9). 미통합 시절 Mock이 no-op이었듯, 실패도 무해하게 흘려보낸다.
 */
@Slf4j
@Component
public class WriteAuditLogRestClientAdapter implements WriteAuditLogUseCase {

    private final RestClient restClient;
    /** 시스템 토큰 발급기 — security.jwt.system-secret 미설정 시 null(전송 생략). */
    private final SystemTokenProvider systemTokenProvider;

    @Autowired
    public WriteAuditLogRestClientAdapter(RestClient.Builder restClientBuilder,
                                          ServiceEndpointsProperties endpoints,
                                          ObjectProvider<SystemTokenProvider> systemTokenProvider) {
        this(restClientBuilder, endpoints, systemTokenProvider.getIfAvailable());
    }

    /** 단위 테스트용 — 해소된 {@link SystemTokenProvider}(또는 null)를 직접 주입한다. */
    WriteAuditLogRestClientAdapter(RestClient.Builder restClientBuilder,
                                   ServiceEndpointsProperties endpoints,
                                   SystemTokenProvider systemTokenProvider) {
        this.restClient = restClientBuilder.baseUrl(endpoints.getAdminBaseUrl()).build();
        this.systemTokenProvider = systemTokenProvider;
    }

    @Override
    public void write(AuditLogWriteRequest request) {
        if (systemTokenProvider == null) {
            // 시크릿 미설정 — 시스템 토큰을 발급할 수 없으면 감사 전송을 생략한다(fire-and-forget).
            log.warn("시스템 토큰 미설정 — 감사 로그 전송 생략(fire-and-forget). actionType={}",
                    request == null ? null : request.actionType());
            return;
        }
        try {
            restClient.post()
                    .uri("/api/v1/system/audit-logs")
                    .headers(headers -> headers.setBearerAuth(systemTokenProvider.issueSystemToken()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            // fire-and-forget: 전송 실패가 AI 작업을 깨지 않도록 경고만 남긴다. 토큰·본문 등 민감정보는 로깅하지 않는다.
            log.warn("감사 로그 전송 실패(무시) — actionType={}, errorType={}",
                    request == null ? null : request.actionType(), e.getClass().getSimpleName());
        }
    }
}
