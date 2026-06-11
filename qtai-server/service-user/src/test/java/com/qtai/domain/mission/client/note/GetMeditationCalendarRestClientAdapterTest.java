package com.qtai.domain.mission.client.note;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * {@link GetMeditationCalendarRestClientAdapter} 단위 테스트 — service-note 묵상 달력 호출을
 * MockRestServiceServer로 흉내낸다 (GET 경로·month 파라미터·응답 매핑·5xx 변환).
 *
 * <p>인증 헤더는 사용자 요청 컨텍스트에서만 전달되므로(ServiceCallAuthForwarder),
 * 컨텍스트가 없는 단위 테스트에서는 헤더 없이 호출되는 것이 정상이다.
 */
class GetMeditationCalendarRestClientAdapterTest {

    private static final String NOTE_BASE = "http://note.test";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private GetMeditationCalendarRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ServiceEndpointsProperties endpoints = new ServiceEndpointsProperties();
        endpoints.setNoteBaseUrl(NOTE_BASE);
        adapter = new GetMeditationCalendarRestClientAdapter(builder, endpoints);
    }

    @Test
    @DisplayName("GET /api/v1/me/meditation-calendar?month=… 를 호출하고 days/summary를 매핑한다")
    void getCalendar_정상() {
        server.expect(requestTo(startsWith(NOTE_BASE + "/api/v1/me/meditation-calendar")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(queryParam("month", "2026-06"))
                .andRespond(withSuccess("""
                        {"success":true,"data":{
                          "month":"2026-06",
                          "days":[
                            {"date":"2026-06-10","saved":true,"savedNoteCount":2,
                             "meditationNoteId":11,"categories":["MEDITATION","PRAYER"]}
                          ],
                          "summary":{"savedDays":1,"savedNoteCount":2,"meditationStreakDays":3}
                        }}""", MediaType.APPLICATION_JSON));

        MeditationCalendarResponse response = adapter.getCalendar(7L, YearMonth.of(2026, 6));

        assertThat(response.month()).isEqualTo("2026-06");
        assertThat(response.days()).hasSize(1);
        assertThat(response.days().get(0).date()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(response.days().get(0).saved()).isTrue();
        assertThat(response.days().get(0).savedNoteCount()).isEqualTo(2);
        assertThat(response.summary().meditationStreakDays()).isEqualTo(3);
        server.verify();
    }

    @Test
    @DisplayName("5xx 응답은 EXTERNAL_API_FAILURE로 변환한다")
    void getCalendar_5xx() {
        server.expect(requestTo(startsWith(NOTE_BASE + "/api/v1/me/meditation-calendar")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.getCalendar(7L, YearMonth.of(2026, 6)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("success=false 봉투는 EXTERNAL_API_FAILURE로 변환한다")
    void getCalendar_실패봉투() {
        server.expect(requestTo(startsWith(NOTE_BASE + "/api/v1/me/meditation-calendar")))
                .andRespond(withSuccess("{\"success\":false,\"data\":null}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> adapter.getCalendar(7L, YearMonth.of(2026, 6)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }
}
