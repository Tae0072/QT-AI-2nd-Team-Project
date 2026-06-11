package com.qtai.domain.ai.client.study;

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
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.HidePublishedGlossaryTermsResult;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.HidePublishedVerseExplanationResult;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsCommand;
import com.qtai.domain.study.api.dto.PublishApprovedGlossaryTermsResult;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationCommand;
import com.qtai.domain.study.api.dto.PublishApprovedVerseExplanationResult;
import java.time.OffsetDateTime;
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
 * {@link VerseExplanationRestClientAdapter} 단위 테스트 — MockRestServiceServer로 service-bible(study) 호출을 흉내낸다.
 *
 * <p>검증: 게시(POST)·숨김(POST /hide)·조회(GET) 매핑, 시스템 토큰 Bearer 주입, 빈 verseIds 단락,
 * 5xx→{@link ErrorCode#EXTERNAL_API_FAILURE}, 토큰 발급기 미설정 실패.
 */
class VerseExplanationRestClientAdapterTest {

    private static final String BIBLE_BASE_URL = "http://bible.test";
    private static final String BASE = BIBLE_BASE_URL + "/api/v1/study/verse-explanations";
    private static final String GLOSSARY_BASE = BIBLE_BASE_URL + "/api/v1/study/glossary-terms";
    // HS256 시크릿은 256비트(32바이트) 이상이어야 한다. 테스트 전용 더미 값(실제 시크릿 아님).
    private static final String TEST_SYSTEM_SECRET = "test-system-secret-0123456789-abcdefghij";

    private MockRestServiceServer server;
    private RestClient.Builder builder;
    private ServiceEndpointsProperties endpoints;
    private VerseExplanationRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        endpoints = new ServiceEndpointsProperties();
        endpoints.setBibleBaseUrl(BIBLE_BASE_URL);
        adapter = new VerseExplanationRestClientAdapter(
                builder, endpoints, new SystemTokenProvider(TEST_SYSTEM_SECRET, 60_000L));
    }

    private static PublishApprovedVerseExplanationCommand publishCommand() {
        return new PublishApprovedVerseExplanationCommand(
                1001L, "요약", "해설 본문", "QT-AI DeepSeek", 5001L,
                OffsetDateTime.parse("2026-06-10T00:00:00Z"));
    }

    private static PublishApprovedGlossaryTermsCommand glossaryPublishCommand() {
        return new PublishApprovedGlossaryTermsCommand(
                5001L,
                "QT-AI DeepSeek",
                OffsetDateTime.parse("2026-06-10T00:00:00Z"),
                List.of(new PublishApprovedGlossaryTermsCommand.Term(
                        1001L,
                        "faith",
                        "trust in God"
                ))
        );
    }

    @Test
    @DisplayName("게시 — POST로 명령을 보내고(시스템 토큰 Bearer) 결과를 매핑한다")
    void 게시_정상() {
        server.expect(requestTo(BASE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"bibleVerseId\":1001,\"aiAssetId\":5001,\"status\":\"APPROVED\"}}",
                        MediaType.APPLICATION_JSON));

        PublishApprovedVerseExplanationResult result = adapter.publishApprovedVerseExplanation(publishCommand());

        assertThat(result.bibleVerseId()).isEqualTo(1001L);
        assertThat(result.aiAssetId()).isEqualTo(5001L);
        assertThat(result.status()).isEqualTo("APPROVED");
        server.verify();
    }

    @Test
    @DisplayName("숨김 — POST /hide로 명령을 보내고 hiddenCount를 매핑한다")
    void 숨김_정상() {
        server.expect(requestTo(BASE + "/hide"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"aiAssetId\":5001,\"hiddenCount\":2}}",
                        MediaType.APPLICATION_JSON));

        HidePublishedVerseExplanationResult result =
                adapter.hidePublishedVerseExplanation(new HidePublishedVerseExplanationCommand(5001L));

        assertThat(result.aiAssetId()).isEqualTo(5001L);
        assertThat(result.hiddenCount()).isEqualTo(2);
        server.verify();
    }

    @Test
    @DisplayName("glossary 게시는 POST로 명령을 보내고 결과를 매핑한다")
    void glossaryPublishSuccess() {
        server.expect(requestTo(GLOSSARY_BASE))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"aiAssetId\":5001,\"publishedCount\":1,\"hiddenCount\":0}}",
                        MediaType.APPLICATION_JSON));

        PublishApprovedGlossaryTermsResult result = adapter.publishApprovedGlossaryTerms(glossaryPublishCommand());

        assertThat(result.aiAssetId()).isEqualTo(5001L);
        assertThat(result.publishedCount()).isEqualTo(1);
        assertThat(result.hiddenCount()).isZero();
        server.verify();
    }

    @Test
    @DisplayName("glossary 숨김은 POST /hide로 명령을 보내고 결과를 매핑한다")
    void glossaryHideSuccess() {
        server.expect(requestTo(GLOSSARY_BASE + "/hide"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"aiAssetId\":5001,\"hiddenCount\":2}}",
                        MediaType.APPLICATION_JSON));

        HidePublishedGlossaryTermsResult result =
                adapter.hidePublishedGlossaryTerms(new HidePublishedGlossaryTermsCommand(5001L));

        assertThat(result.aiAssetId()).isEqualTo(5001L);
        assertThat(result.hiddenCount()).isEqualTo(2);
        server.verify();
    }

    @Test
    @DisplayName("조회 — GET으로 verseIds를 보내고 승인 해설 목록을 매핑한다")
    void 조회_정상() {
        server.expect(requestTo(startsWith(BASE + "?verseIds=")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header("Authorization", startsWith("Bearer ")))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":[{\"verseId\":1001,\"summary\":\"요약\","
                                + "\"explanation\":\"해설\",\"sourceLabel\":\"QT-AI DeepSeek\",\"aiAssetId\":5001}]}",
                        MediaType.APPLICATION_JSON));

        List<ApprovedVerseExplanationResponse> approved = adapter.listApprovedByVerseIds(List.of(1001L));

        assertThat(approved).hasSize(1);
        assertThat(approved.get(0).verseId()).isEqualTo(1001L);
        assertThat(approved.get(0).aiAssetId()).isEqualTo(5001L);
        server.verify();
    }

    @Test
    @DisplayName("조회 — 빈 verseIds는 HTTP 호출 없이 빈 목록")
    void 조회_빈목록_단락() {
        assertThat(adapter.listApprovedByVerseIds(List.of())).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("게시 5xx는 EXTERNAL_API_FAILURE로 변환한다")
    void 게시_5xx() {
        server.expect(requestTo(BASE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.publishApprovedVerseExplanation(publishCommand()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("glossary 게시 5xx는 EXTERNAL_API_FAILURE로 변환한다")
    void glossaryPublish5xx() {
        server.expect(requestTo(GLOSSARY_BASE))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.publishApprovedGlossaryTerms(glossaryPublishCommand()))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("glossary 숨김 5xx는 EXTERNAL_API_FAILURE로 변환한다")
    void glossaryHide5xx() {
        server.expect(requestTo(GLOSSARY_BASE + "/hide"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.hidePublishedGlossaryTerms(new HidePublishedGlossaryTermsCommand(5001L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("시스템 토큰 발급기가 없으면(시크릿 미설정) EXTERNAL_API_FAILURE로 실패한다")
    void 시스템토큰_미설정_실패() {
        VerseExplanationRestClientAdapter noTokenAdapter =
                new VerseExplanationRestClientAdapter(builder, endpoints, (SystemTokenProvider) null);

        assertThatThrownBy(() ->
                noTokenAdapter.hidePublishedVerseExplanation(new HidePublishedVerseExplanationCommand(5001L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }
}
