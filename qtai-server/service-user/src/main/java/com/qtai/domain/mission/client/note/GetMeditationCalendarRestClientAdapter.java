package com.qtai.domain.mission.client.note;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.ServiceCallAuthForwarder;
import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

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
@Component
public class GetMeditationCalendarRestClientAdapter implements GetMeditationCalendarUseCase {

    private static final ParameterizedTypeReference<ApiResponse<MeditationCalendarResponse>> CALENDAR_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;

    public GetMeditationCalendarRestClientAdapter(RestClient.Builder restClientBuilder,
                                                  ServiceEndpointsProperties endpoints) {
        this.restClient = restClientBuilder.baseUrl(endpoints.getNoteBaseUrl()).build();
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
            return unwrap(body);
        } catch (RestClientException e) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
    }

    private MeditationCalendarResponse unwrap(ApiResponse<MeditationCalendarResponse> body) {
        if (body == null || !body.success() || body.data() == null) {
            throw new BusinessException(ErrorCode.EXTERNAL_API_FAILURE);
        }
        return body.data();
    }
}
