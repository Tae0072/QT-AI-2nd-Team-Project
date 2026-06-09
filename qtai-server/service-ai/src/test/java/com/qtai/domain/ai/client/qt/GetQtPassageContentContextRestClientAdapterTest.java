package com.qtai.domain.ai.client.qt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.SystemTokenProvider;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * {@link GetQtPassageContentContextRestClientAdapter} 단위 테스트 — MockRestServiceServer로 service-bible(qt) 호출을 흉내낸다.
 *
 * <p>검증: 본문 매핑(verseIds/published 포함), 시스템 토큰 Bearer 주입, getContentContext 404→
 * {@link ErrorCode#QT_PASSAGE_NOT_FOUND}, findContentContextByDate 404→{@link Optional#empty()},
 * 5xx→{@link ErrorCode#EXTERNAL_API_FAILURE}, 토큰 발급기 미설정 실패.
 */
class GetQtPassageContentContextRestClientAdapterTest {

    private static final String BIBLE_BASE_URL = "http://bible.test";
    // HS256 시크릿은 256비트(32바이트) 이상이어야 한다. 테스트 전용 더미 값(실제 시크릿 아님).
    private static final String TEST_SYSTEM_SECRET = "test-system-secret-0123456789-abcdefghij";

    private MockRestServiceServer server;
    private RestClient.Builder builder;
    private ServiceEndpointsProperties endpoints;
    private GetQtPassageContentContextRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        endpoints = new ServiceEndpointsProperties();
        endpoints.setBibleBaseUrl(BIBLE_BASE_URL);
        adapter = new GetQtPassageContentContextRestClientAdapter(
                builder, endpoints, new SystemTokenProvider(TEST_SYSTEM_SECRET, 60_000L));
    }

    @Test
    @DisplayName("id 조회 — 컨텍스트 본문(verseIds/published)을 매핑하고 시스템 토큰을 Bearer로 보낸다")
    void id_조회_정상() {
        server.expect(requestTo(BIBLE_BASE_URL + "/api/v1/qt/passages/101/content-context"))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"qtPassageId\":101,\"qtDate\":\"2026-06-10\","
                                + "\"title\":\"오늘의 QT\",\"verseIds\":[1001,1002],\"published\":true}}",
                        MediaType.APPLICATION_JSON));

        QtPassageContentContext context = adapter.getContentContext(101L);

        assertThat(context.qtPassageId()).isEqualTo(101L);
        assertThat(context.qtDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(context.verseIds()).containsExactly(1001L, 1002L);
        assertThat(context.published()).isTrue();
        server.verify();
    }

    @Test
    @DisplayName("id 조회 — 404는 QT_PASSAGE_NOT_FOUND로 변환한다")
    void id_조회_404() {
        server.expect(requestTo(BIBLE_BASE_URL + "/api/v1/qt/passages/999/content-context"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.getContentContext(999L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("날짜 조회 — 본문이 있으면 Optional로 감싼 컨텍스트를 돌려준다")
    void 날짜_조회_정상() {
        server.expect(requestTo(startsWith(BIBLE_BASE_URL + "/api/v1/qt/content-context")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"qtPassageId\":101,\"qtDate\":\"2026-06-10\","
                                + "\"title\":\"오늘의 QT\",\"verseIds\":[1001],\"published\":true}}",
                        MediaType.APPLICATION_JSON));

        Optional<QtPassageContentContext> context = adapter.findContentContextByDate(LocalDate.of(2026, 6, 10));

        assertThat(context).isPresent();
        assertThat(context.get().qtPassageId()).isEqualTo(101L);
        server.verify();
    }

    @Test
    @DisplayName("날짜 조회 — 404(해당 날짜 본문 없음)는 Optional.empty()로 변환한다")
    void 날짜_조회_404_empty() {
        server.expect(requestTo(startsWith(BIBLE_BASE_URL + "/api/v1/qt/content-context")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        Optional<QtPassageContentContext> context = adapter.findContentContextByDate(LocalDate.of(2026, 6, 10));

        assertThat(context).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("5xx 응답은 EXTERNAL_API_FAILURE로 변환한다")
    void 응답_5xx() {
        server.expect(requestTo(BIBLE_BASE_URL + "/api/v1/qt/passages/101/content-context"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.getContentContext(101L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("시스템 토큰 발급기가 없으면(시크릿 미설정) EXTERNAL_API_FAILURE로 실패한다")
    void 시스템토큰_미설정_실패() {
        GetQtPassageContentContextRestClientAdapter noTokenAdapter =
                new GetQtPassageContentContextRestClientAdapter(builder, endpoints, (SystemTokenProvider) null);

        assertThatThrownBy(() -> noTokenAdapter.getContentContext(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }
}
