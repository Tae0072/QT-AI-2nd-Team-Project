package com.qtai.domain.member.client.note;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * {@link PurgeMemberNoteDataRestClientAdapter} 단위 테스트 — service-note purge 호출을 MockRestServiceServer로 흉내낸다.
 *
 * <p>note 어댑터에서 공통 패턴(삭제 행 수 매핑·시스템 토큰 Bearer·5xx·토큰 미설정)을 대표 검증한다.
 */
class PurgeMemberNoteDataRestClientAdapterTest {

    private static final String NOTE_BASE = "http://note.test";
    // HS256 시크릿은 256비트(32바이트) 이상이어야 한다. 테스트 전용 더미 값(실제 시크릿 아님).
    private static final String TEST_SYSTEM_SECRET = "test-system-secret-0123456789-abcdefghij";

    private RestClient.Builder builder;
    private MockRestServiceServer server;
    private ServiceEndpointsProperties endpoints;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        endpoints = new ServiceEndpointsProperties();
        endpoints.setNoteBaseUrl(NOTE_BASE);
    }

    @Test
    @DisplayName("POST로 삭제 요청하고(시스템 토큰 Bearer) 삭제 행 수를 매핑한다")
    void purge_정상() {
        var adapter = new PurgeMemberNoteDataRestClientAdapter(
                builder, endpoints, new SystemTokenProvider(TEST_SYSTEM_SECRET, 60_000L));

        server.expect(requestTo(startsWith(NOTE_BASE + "/api/v1/notes/purge")))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess("{\"success\":true,\"data\":3}", MediaType.APPLICATION_JSON));

        assertThat(adapter.purgeByMemberId(7L)).isEqualTo(3);
        server.verify();
    }

    @Test
    @DisplayName("5xx 응답은 EXTERNAL_API_FAILURE로 변환한다")
    void purge_5xx() {
        var adapter = new PurgeMemberNoteDataRestClientAdapter(
                builder, endpoints, new SystemTokenProvider(TEST_SYSTEM_SECRET, 60_000L));

        server.expect(requestTo(startsWith(NOTE_BASE + "/api/v1/notes/purge")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.purgeByMemberId(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("시스템 토큰 발급기가 없으면(시크릿 미설정) EXTERNAL_API_FAILURE로 실패한다")
    void purge_토큰_미설정() {
        var adapter = new PurgeMemberNoteDataRestClientAdapter(builder, endpoints, (SystemTokenProvider) null);

        assertThatThrownBy(() -> adapter.purgeByMemberId(7L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }
}
