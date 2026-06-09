package com.qtai.external.bible;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.dto.BibleBookResponse;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestToUriTemplate;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.GET;

/**
 * bible-service HTTP 클라이언트 단위 테스트 — 토큰 주입·envelope 언랩·오류 역매핑.
 */
class BibleServiceClientTest {

    private static final String TOKEN = "gw-test-token"; // gitleaks:allow — 테스트 전용 더미 토큰

    private MockRestServiceServer server;
    private RestClient restClient;
    private BibleServiceClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://bible-service");
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        client = new BibleServiceClient(restClient, TOKEN, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    @DisplayName("listBibleBooks — X-Gateway-Token 주입 + envelope data 언랩")
    void listBibleBooks_sendsTokenAndUnwrapsData() {
        server.expect(requestToUriTemplate("http://bible-service/api/v1/bible/books"))
                .andExpect(method(GET))
                .andExpect(header("X-Gateway-Token", TOKEN))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":[{\"id\":1,\"testament\":\"OLD\",\"code\":\"GEN\","
                                + "\"koreanName\":\"\\uCC3D\\uC138\\uAE30\",\"englishName\":\"Genesis\",\"displayOrder\":1}]}",
                        MediaType.APPLICATION_JSON));

        List<BibleBookResponse> books = client.listBibleBooks();

        assertThat(books).hasSize(1);
        assertThat(books.get(0).code()).isEqualTo("GEN");
        server.verify();
    }

    @Test
    @DisplayName("getVerses(ids) — /verses/by-ids?ids= 호출 + 목록 언랩")
    void getVersesByIds_buildsIdsQueryAndUnwraps() {
        server.expect(method(GET))
                .andExpect(queryParam("ids", "10", "11"))
                .andExpect(header("X-Gateway-Token", TOKEN))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":[{\"id\":10,\"bookCode\":\"GEN\",\"chapterNo\":1,"
                                + "\"verseNo\":1,\"koreanText\":\"x\",\"englishText\":\"y\"}]}",
                        MediaType.APPLICATION_JSON));

        List<BibleVerseResponse> verses = client.getVerses(List.of(10L, 11L));

        assertThat(verses).hasSize(1);
        assertThat(verses.get(0).id()).isEqualTo(10L);
        server.verify();
    }

    @Test
    @DisplayName("getVerses(범위) — bookCode/chapter/verseFrom/verseTo 쿼리 + 언랩")
    void getVersesRange_buildsQueryAndUnwraps() {
        server.expect(method(GET))
                .andExpect(queryParam("bookCode", "GEN"))
                .andExpect(queryParam("chapter", "1"))
                .andExpect(queryParam("verseFrom", "1"))
                .andExpect(queryParam("verseTo", "3"))
                .andExpect(header("X-Gateway-Token", TOKEN))
                .andRespond(withSuccess("{\"success\":true,\"data\":{}}", MediaType.APPLICATION_JSON));

        var range = client.getVerses("GEN", 1, 1, 3);

        assertThat(range).isNotNull();
        server.verify();
    }

    @Test
    @DisplayName("빈 ID 입력은 네트워크 호출 없이 빈 결과(in-process 계약)")
    void getVersesByIds_emptyInput_returnsEmptyWithoutCall() {
        // server에 어떤 expect도 걸지 않음 → 호출이 일어나면 verify에서 실패
        List<BibleVerseResponse> verses = client.getVerses(List.of());

        assertThat(verses).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("일시 오류(503) 2회 후 200 — 재시도로 복구")
    void transientServerError_retriesThenSucceeds() {
        BibleServiceClient retryClient = new BibleServiceClient(
                restClient, TOKEN, new ObjectMapper().findAndRegisterModules(), 3, 0);
        server.expect(ExpectedCount.times(2), requestToUriTemplate("http://bible-service/api/v1/bible/books"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(ExpectedCount.once(), requestToUriTemplate("http://bible-service/api/v1/bible/books"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":[{\"id\":1,\"testament\":\"OLD\",\"code\":\"GEN\","
                                + "\"koreanName\":\"x\",\"englishName\":\"Genesis\",\"displayOrder\":1}]}",
                        MediaType.APPLICATION_JSON));

        List<BibleBookResponse> books = retryClient.listBibleBooks();

        assertThat(books).hasSize(1);
        server.verify();
    }

    @Test
    @DisplayName("일시 오류(503) 재시도 소진 → BusinessException(EXTERNAL_API_FAILURE)")
    void transientServerError_exhaustsRetries_throwsExternalFailure() {
        BibleServiceClient retryClient = new BibleServiceClient(
                restClient, TOKEN, new ObjectMapper().findAndRegisterModules(), 3, 0);
        server.expect(ExpectedCount.times(3), requestToUriTemplate("http://bible-service/api/v1/bible/books"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(retryClient::listBibleBooks)
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
        server.verify();
    }

    @Test
    @DisplayName("오류 응답(404 B0002) → error 코드 역매핑한 BusinessException(BIBLE_VERSE_NOT_FOUND), 재시도 안 함")
    void errorResponse_mapsToBusinessException() {
        server.expect(method(GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"success\":false,\"data\":null,\"error\":{\"code\":\"B0002\","
                                + "\"message\":\"\\uC131\\uACBD \\uAD6C\\uC808\\uC744 \\uCC3E\\uC744 \\uC218 \\uC5C6\\uC2B5\\uB2C8\\uB2E4.\"}}"));

        assertThatThrownBy(() -> client.getVerses(List.of(999L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIBLE_VERSE_NOT_FOUND);
    }
}
