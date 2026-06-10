package com.qtai.domain.ai.client.bible;

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
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/**
 * {@link GetBibleVerseRestClientAdapter} 단위 테스트 — MockRestServiceServer로 service-bible 호출을 흉내낸다.
 *
 * <p>검증: 단건/다건 정상 응답 매핑, 404→{@link ErrorCode#BIBLE_VERSE_NOT_FOUND},
 * 5xx→{@link ErrorCode#EXTERNAL_API_FAILURE}, 빈 목록은 HTTP 호출 없이 빈 결과,
 * 배치 호출이 SYSTEM_BATCH 시스템 토큰을 Bearer 헤더로 실어 보내는지, 토큰 발급기 미설정 시 실패.
 */
class GetBibleVerseRestClientAdapterTest {

    private static final String BIBLE_BASE_URL = "http://bible.test";
    // HS256 시크릿은 256비트(32바이트) 이상이어야 한다. 테스트 전용 더미 값(실제 시크릿 아님).
    private static final String TEST_SYSTEM_SECRET = "test-system-secret-0123456789-abcdefghij";

    private MockRestServiceServer server;
    private RestClient.Builder builder;
    private ServiceEndpointsProperties endpoints;
    private GetBibleVerseRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        endpoints = new ServiceEndpointsProperties();
        endpoints.setBibleBaseUrl(BIBLE_BASE_URL);
        adapter = new GetBibleVerseRestClientAdapter(
                builder, endpoints, new SystemTokenProvider(TEST_SYSTEM_SECRET, 60_000L));
    }

    @Test
    @DisplayName("다건 조회 — by-ids 엔드포인트를 호출하고 본문을 매핑한다")
    void 다건_정상() {
        server.expect(requestTo(startsWith(BIBLE_BASE_URL + "/api/v1/bible/verses/by-ids")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":[{\"id\":10,\"bookCode\":\"GEN\",\"chapterNo\":1,\"verseNo\":1}]}",
                        MediaType.APPLICATION_JSON));

        List<BibleVerseResponse> verses = adapter.getVerses(List.of(10L));

        assertThat(verses).hasSize(1);
        assertThat(verses.get(0).id()).isEqualTo(10L);
        assertThat(verses.get(0).bookCode()).isEqualTo("GEN");
        server.verify();
    }

    @Test
    @DisplayName("단건 조회 — verses/{id} 엔드포인트를 호출하고 본문을 매핑한다")
    void 단건_정상() {
        server.expect(requestTo(BIBLE_BASE_URL + "/api/v1/bible/verses/10"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"id\":10,\"bookCode\":\"GEN\",\"chapterNo\":1,\"verseNo\":2}}",
                        MediaType.APPLICATION_JSON));

        BibleVerseResponse verse = adapter.getVerse(10L);

        assertThat(verse.id()).isEqualTo(10L);
        assertThat(verse.verseNo()).isEqualTo(2);
        server.verify();
    }

    @Test
    @DisplayName("배치 호출은 SYSTEM_BATCH 시스템 토큰을 Bearer 헤더로 실어 보낸다")
    void 시스템토큰_헤더_주입() {
        server.expect(requestTo(startsWith(BIBLE_BASE_URL + "/api/v1/bible/verses/by-ids")))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":[{\"id\":1,\"bookCode\":\"GEN\",\"chapterNo\":1,\"verseNo\":1}]}",
                        MediaType.APPLICATION_JSON));

        adapter.getVerses(List.of(1L));

        server.verify();
    }

    @Test
    @DisplayName("404 응답은 BIBLE_VERSE_NOT_FOUND로 변환한다")
    void 응답_404() {
        server.expect(requestTo(startsWith(BIBLE_BASE_URL + "/api/v1/bible/verses/by-ids")))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.getVerses(List.of(99L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIBLE_VERSE_NOT_FOUND);
    }

    @Test
    @DisplayName("5xx 응답은 EXTERNAL_API_FAILURE로 변환한다")
    void 응답_5xx() {
        server.expect(requestTo(startsWith(BIBLE_BASE_URL + "/api/v1/bible/verses/by-ids")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.getVerses(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("빈 목록은 HTTP 호출 없이 빈 결과를 돌려준다")
    void 빈_목록_단락() {
        assertThat(adapter.getVerses(List.of())).isEmpty();
        server.verify(); // 기대한 요청이 없음 = 호출이 없었음
    }

    @Test
    @DisplayName("시스템 토큰 발급기가 없으면(시크릿 미설정) EXTERNAL_API_FAILURE로 실패한다")
    void 시스템토큰_미설정_실패() {
        GetBibleVerseRestClientAdapter noTokenAdapter =
                new GetBibleVerseRestClientAdapter(builder, endpoints, (SystemTokenProvider) null);

        assertThatThrownBy(() -> noTokenAdapter.getVerse(1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }
}
