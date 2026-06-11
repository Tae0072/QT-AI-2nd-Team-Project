package com.qtai.domain.mission.client.note;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.ServiceCallAuthForwarder;
import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.time.YearMonth;

/**
 * note 도메인 {@link GetMeditationCalendarUseCase}의 service-user 구현 — service-note HTTP 호출 어댑터.
 *
 * <p>마이페이지 통계 위젯({@code MyPageController.loadStats})과 미션 진행률
 * ({@code MissionProgressCalculator})이 월간 묵상 집계를 소비한다. note는 service-note 소관이라
 * service-user는 api 계약만 가져와 client 어댑터로 구현한다(CLAUDE.md §4). 이 어댑터가 기존
 * {@code GetMeditationCalendarUseCaseMock}을 대체한다.
 *
 * <p>인증(사용자 요청 경로): 대시보드 요청 맥락에서 호출되므로 들어온 사용자 JWT를
 * {@link ServiceCallAuthForwarder}로 그대로 전달한다(회의록 2026-06-09 §5/§81). 수신측
 * ({@code GET /api/v1/me/meditation-calendar})은 토큰의 {@code @AuthenticationPrincipal}로 회원을
 * 식별하므로 {@code memberId} 파라미터는 전송하지 않는다 — 호출자 토큰 주체와 동일하다는 전제이며,
 * 사용자 요청 컨텍스트가 없는 배치(SYSTEM_BATCH) 경로에서는 사용할 수 없다.
 *
 * <p>오류: 호출 실패는 광범위 catch 대신 {@link RestClientException}만 잡아 공통 예외
 * ({@link ErrorCode#EXTERNAL_API_FAILURE})로 감싼다(§9). 대시보드는 위젯별 부분 실패 정책이라
 * 호출측에서 widgetErrors로 격리한다. 토큰은 로그/예외 메시지에 남기지 않는다(§9).
 */
@Slf4j
@Component
public class GetMeditationCalendarRestClientAdapter implements GetMeditationCalendarUseCase {

    private static final ParameterizedTypeReference<ApiResponse<MeditationCalendarResponse>> CALENDAR_TYPE =
            new ParameterizedTypeReference<>() {
            };

    /** 대시보드 위젯 경로 — 부분 실패가 허용되므로 짧은 타임아웃으로 빠르게 실패시킨다. */
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);

    private final RestClient restClient;

    @Autowired
    public GetMeditationCalendarRestClientAdapter(RestClient.Builder restClientBuilder,
                                                  ServiceEndpointsProperties endpoints) {
        this(restClientBuilder, endpoints, true);
    }

    /**
     * @param applyTimeouts 운영 경로는 true(타임아웃 팩토리 적용). 테스트는 false —
     *                      MockRestServiceServer가 빌더에 심는 mock 팩토리를 덮어쓰지 않기 위함.
     */
    GetMeditationCalendarRestClientAdapter(RestClient.Builder restClientBuilder,
                                           ServiceEndpointsProperties endpoints,
                                           boolean applyTimeouts) {
        RestClient.Builder builder = restClientBuilder.baseUrl(endpoints.getNoteBaseUrl());
        if (applyTimeouts) {
            // 공용 restClientBuilder에는 타임아웃이 없어(무한 대기) 어댑터 단위로 명시한다.
            ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                    .withConnectTimeout(CONNECT_TIMEOUT)
                    .withReadTimeout(READ_TIMEOUT);
            builder = builder.requestFactory(ClientHttpRequestFactories.get(settings));
        }
        this.restClient = builder.build();
    }

    @Override
    public MeditationCalendarResponse getCalendar(Long memberId, YearMonth month) {
        try {
            ApiResponse<MeditationCalendarResponse> body = restClient.get()
                    .uri(uri -> uri.path("/api/v1/me/meditation-calendar")
                            .queryParam("month", month.toString())
                            .build())
                    .headers(ServiceCallAuthForwarder::forward)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
                    })
                    .body(CALENDAR_TYPE);
            return unwrap(body, month);
        } catch (RestClientException e) {
            // BusinessException이 cause 체인을 받지 않아 원인은 여기서 로그로 보존한다
            // (§9 — 토큰·Authorization 헤더는 예외 메시지에 포함되지 않는 일반 HTTP 오류만 기록).
            log.warn("묵상 달력 호출 실패: month={}, cause={}", month, e.toString());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    private MeditationCalendarResponse unwrap(ApiResponse<MeditationCalendarResponse> body, YearMonth month) {
        if (body == null || !body.success() || body.data() == null) {
            log.warn("묵상 달력 응답 봉투 비정상: month={}, success={}", month,
                    body == null ? null : body.success());
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return body.data();
    }
}
