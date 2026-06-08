package com.qtai.domain.ai.client.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.qtai.domain.ai.client.AiClientException;
import com.qtai.domain.ai.client.AiClientException.FailureCode;
import com.qtai.domain.ai.client.admin.AdminAuthClient;
import com.qtai.domain.ai.client.admin.AdminAuthClientHttpAdapter;
import com.qtai.domain.ai.client.audit.AuditLogClient;
import com.qtai.domain.ai.client.audit.AuditLogClientHttpAdapter;
import com.qtai.domain.ai.client.bible.BibleVerseClient;
import com.qtai.domain.ai.client.bible.BibleVerseClientHttpAdapter;
import com.qtai.domain.ai.client.qt.QtContextClient;
import com.qtai.domain.ai.client.qt.QtContextClientHttpAdapter;
import com.qtai.domain.ai.client.study.StudyPublishClient;
import com.qtai.domain.ai.client.study.StudyPublishClientHttpAdapter;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class AiHttpClientAdapterContractTest {

    private static final String SERVICE_TOKEN = "test-service-token";
    private static final String TRACEPARENT = "00-11111111111111111111111111111111-2222222222222222-01";

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void tearDown() throws IOException {
        MDC.clear();
        server.shutdown();
    }

    @Test
    void qtAdapterSendsContextAndTodayStatusRequests() throws Exception {
        server.enqueue(successResponse(Map.of(
                "passageId", 35L,
                "bibleBook", "JOHN",
                "chapter", 3,
                "startVerse", 16,
                "endVerse", 16,
                "passageReference", "John 3:16",
                "title", "Today QT",
                "summary", "summary",
                "passageContext", "allowed metadata context"
        )));
        server.enqueue(successResponse(Map.of(
                "qtDate", "2026-06-08",
                "exists", true,
                "passageId", 35L,
                "cacheStatus", "HIT"
        )));
        MDC.put("traceparent", TRACEPARENT);

        QtContextClient client = new QtContextClientHttpAdapter(objectMapper, properties());

        assertThat(client.getQtContext(10L, 35L).passageContext()).isEqualTo("allowed metadata context");
        assertThat(client.getTodayQtPassageStatus(LocalDate.of(2026, 6, 8)).cacheStatus())
                .isEqualTo(QtContextClient.CacheStatus.HIT);

        RecordedRequest contextRequest = server.takeRequest();
        assertThat(contextRequest.getMethod()).isEqualTo("GET");
        assertThat(contextRequest.getPath()).isEqualTo("/api/v1/system/qt/passages/35/context");
        assertThat(contextRequest.getPath()).doesNotContain("viewerId");
        assertCommonHeaders(contextRequest);
        assertThat(contextRequest.getHeader("traceparent")).isEqualTo(TRACEPARENT);

        RecordedRequest todayRequest = server.takeRequest();
        assertThat(todayRequest.getMethod()).isEqualTo("GET");
        assertThat(todayRequest.getRequestUrl().encodedPath()).isEqualTo("/api/v1/system/qt/passages/today/status");
        assertThat(todayRequest.getRequestUrl().queryParameter("date")).isEqualTo("2026-06-08");
        assertCommonHeaders(todayRequest);
    }

    @Test
    void bibleAdapterSendsSingleBatchAndRangeRequests() throws Exception {
        server.enqueue(successResponse(verse(16L, 16)));
        server.enqueue(successResponse(Map.of("verses", List.of(verse(16L, 16), verse(17L, 17)))));
        server.enqueue(successResponse(Map.of(
                "bibleBook", "JOHN",
                "chapter", 3,
                "startVerse", 16,
                "endVerse", 17,
                "verses", List.of(verse(16L, 16), verse(17L, 17))
        )));

        BibleVerseClient client = new BibleVerseClientHttpAdapter(objectMapper, properties());

        assertThat(client.getVerse(16L).reference()).isEqualTo("JOHN 3:16");
        assertThat(client.getVersesByIds(List.of(16L, 17L))).hasSize(2);
        assertThat(client.getVersesInRange("JOHN", 3, 16, 17).verses()).hasSize(2);

        RecordedRequest singleRequest = server.takeRequest();
        assertThat(singleRequest.getMethod()).isEqualTo("GET");
        assertThat(singleRequest.getPath()).isEqualTo("/api/v1/system/bible/verses/16");
        assertCommonHeaders(singleRequest);

        RecordedRequest batchRequest = server.takeRequest();
        assertThat(batchRequest.getMethod()).isEqualTo("POST");
        assertThat(batchRequest.getPath()).isEqualTo("/api/v1/system/bible/verses:batch");
        assertCommonHeaders(batchRequest);
        JsonNode verseIds = bodyJson(batchRequest).get("verseIds");
        assertThat(verseIds).hasSize(2);
        assertThat(verseIds.get(0).asLong()).isEqualTo(16L);
        assertThat(verseIds.get(1).asLong()).isEqualTo(17L);

        RecordedRequest rangeRequest = server.takeRequest();
        assertThat(rangeRequest.getMethod()).isEqualTo("GET");
        assertThat(rangeRequest.getRequestUrl().encodedPath()).isEqualTo("/api/v1/system/bible/verses");
        assertThat(rangeRequest.getRequestUrl().queryParameter("book")).isEqualTo("JOHN");
        assertThat(rangeRequest.getRequestUrl().queryParameter("chapter")).isEqualTo("3");
        assertThat(rangeRequest.getRequestUrl().queryParameter("startVerse")).isEqualTo("16");
        assertThat(rangeRequest.getRequestUrl().queryParameter("endVerse")).isEqualTo("17");
        assertCommonHeaders(rangeRequest);
    }

    @Test
    void writeAdaptersSendIdempotencyKeysAndBodies() throws Exception {
        server.enqueue(voidSuccessResponse());
        server.enqueue(voidSuccessResponse());
        server.enqueue(voidSuccessResponse());

        AiClientProperties properties = properties();
        StudyPublishClient studyClient = new StudyPublishClientHttpAdapter(objectMapper, properties);
        AuditLogClient auditClient = new AuditLogClientHttpAdapter(objectMapper, properties);

        studyClient.publishApprovedVerseExplanation(new StudyPublishClient.PublishVerseExplanationCommand(
                16L,
                "summary",
                "explanation",
                "QT-AI",
                100L,
                OffsetDateTime.parse("2026-06-08T00:00:00+09:00")
        ));
        studyClient.hidePublishedVerseExplanation(new StudyPublishClient.HideVerseExplanationCommand(100L));
        auditClient.writeAuditLog(new AuditLogClient.AuditLogCommand(
                1L,
                "SYSTEM_BATCH",
                null,
                "AI batch",
                "AI_ASSET_APPROVE",
                "AI_GENERATED_ASSET",
                100L,
                "{}",
                "{}"
        ));

        RecordedRequest publishRequest = server.takeRequest();
        assertThat(publishRequest.getMethod()).isEqualTo("POST");
        assertThat(publishRequest.getPath()).isEqualTo("/api/v1/system/study/verse-explanations:publish");
        assertCommonHeaders(publishRequest);
        assertIdempotencyKey(publishRequest);
        assertThat(bodyJson(publishRequest).get("aiAssetId").asLong()).isEqualTo(100L);

        RecordedRequest hideRequest = server.takeRequest();
        assertThat(hideRequest.getMethod()).isEqualTo("POST");
        assertThat(hideRequest.getPath()).isEqualTo("/api/v1/system/study/verse-explanations:hide");
        assertCommonHeaders(hideRequest);
        assertIdempotencyKey(hideRequest);
        assertThat(bodyJson(hideRequest).get("aiAssetId").asLong()).isEqualTo(100L);

        RecordedRequest auditRequest = server.takeRequest();
        assertThat(auditRequest.getMethod()).isEqualTo("POST");
        assertThat(auditRequest.getPath()).isEqualTo("/api/v1/system/audit/logs");
        assertCommonHeaders(auditRequest);
        assertIdempotencyKey(auditRequest);
        assertThat(bodyJson(auditRequest).get("actorType").asText()).isEqualTo("SYSTEM_BATCH");
    }

    @Test
    void adminAuthAdapterSendsRoleQueries() throws Exception {
        server.enqueue(successResponse(adminResult(AdminAuthClient.AdminRole.REVIEWER)));
        server.enqueue(successResponse(adminResult(AdminAuthClient.AdminRole.SUPER_ADMIN)));
        server.enqueue(successResponse(adminResult(AdminAuthClient.AdminRole.REVIEWER)));

        AdminAuthClient client = new AdminAuthClientHttpAdapter(objectMapper, properties());

        assertThat(client.getActiveAdmin(10L).adminRole()).isEqualTo(AdminAuthClient.AdminRole.REVIEWER);
        assertThat(client.verifyRole(10L, AdminAuthClient.AdminRole.SUPER_ADMIN).adminRole())
                .isEqualTo(AdminAuthClient.AdminRole.SUPER_ADMIN);
        assertThat(client.verifyAnyRole(10L, List.of(
                AdminAuthClient.AdminRole.REVIEWER,
                AdminAuthClient.AdminRole.SUPER_ADMIN
        )).memberId()).isEqualTo(10L);

        RecordedRequest activeRequest = server.takeRequest();
        assertThat(activeRequest.getMethod()).isEqualTo("GET");
        assertThat(activeRequest.getRequestUrl().encodedPath()).isEqualTo("/api/v1/system/admin/auth/active");
        assertThat(activeRequest.getRequestUrl().queryParameter("memberId")).isEqualTo("10");

        RecordedRequest verifyRequest = server.takeRequest();
        assertThat(verifyRequest.getMethod()).isEqualTo("GET");
        assertThat(verifyRequest.getRequestUrl().encodedPath()).isEqualTo("/api/v1/system/admin/auth/verify");
        assertThat(verifyRequest.getRequestUrl().queryParameter("memberId")).isEqualTo("10");
        assertThat(verifyRequest.getRequestUrl().queryParameter("role")).isEqualTo("SUPER_ADMIN");

        RecordedRequest verifyAnyRequest = server.takeRequest();
        assertThat(verifyAnyRequest.getMethod()).isEqualTo("GET");
        assertThat(verifyAnyRequest.getRequestUrl().encodedPath()).isEqualTo("/api/v1/system/admin/auth/verify-any");
        assertThat(verifyAnyRequest.getRequestUrl().queryParameter("memberId")).isEqualTo("10");
        assertThat(verifyAnyRequest.getRequestUrl().queryParameter("roles")).isEqualTo("REVIEWER,SUPER_ADMIN");
    }

    @Test
    void errorEnvelopeIsMappedToAiClientException() {
        server.enqueue(errorResponse("FORBIDDEN", "role is not allowed", 200));

        BibleVerseClient client = new BibleVerseClientHttpAdapter(objectMapper, properties());

        assertAiClientFailure(
                () -> client.getVerse(16L),
                FailureCode.FORBIDDEN,
                "bible",
                "role is not allowed"
        );
    }

    @Test
    void httpStatusErrorEnvelopeTakesPriorityOverStatusFallback() {
        server.enqueue(errorResponse("VALIDATION_FAILED", "invalid role", 403));

        AdminAuthClient client = new AdminAuthClientHttpAdapter(objectMapper, properties());

        assertAiClientFailure(
                () -> client.verifyRole(10L, AdminAuthClient.AdminRole.REVIEWER),
                FailureCode.VALIDATION_FAILED,
                "admin-auth",
                "invalid role"
        );
    }

    @Test
    void httpStatusFailuresUseSharedFailureModel() {
        server.enqueue(statusResponse(401));
        server.enqueue(statusResponse(403));
        server.enqueue(statusResponse(404));
        server.enqueue(statusResponse(429));
        server.enqueue(statusResponse(500));

        BibleVerseClient client = new BibleVerseClientHttpAdapter(objectMapper, properties());

        assertAiClientFailure(() -> client.getVerse(1L), FailureCode.UNAUTHORIZED, "bible", null);
        assertAiClientFailure(() -> client.getVerse(2L), FailureCode.FORBIDDEN, "bible", null);
        assertAiClientFailure(() -> client.getVerse(3L), FailureCode.NOT_FOUND, "bible", null);
        assertAiClientFailure(() -> client.getVerse(4L), FailureCode.RATE_LIMITED, "bible", null);
        assertAiClientFailure(() -> client.getVerse(5L), FailureCode.DOWNSTREAM_ERROR, "bible", null);
    }

    @Test
    void malformedEnvelopeIsMappedToResponseMappingFailure() {
        server.enqueue(jsonResponse(200, "{not-json"));

        BibleVerseClient client = new BibleVerseClientHttpAdapter(objectMapper, properties());

        assertAiClientFailure(
                () -> client.getVerse(16L),
                FailureCode.RESPONSE_MAPPING_FAILED,
                "bible",
                "response envelope mapping failed"
        );
    }

    @Test
    void timeoutIsMappedToTimeoutFailure() {
        AiClientProperties properties = properties();
        properties.setTimeoutMs(50);
        server.enqueue(successResponse(verse(16L, 16))
                .setBodyDelay(500, TimeUnit.MILLISECONDS));

        BibleVerseClient client = new BibleVerseClientHttpAdapter(objectMapper, properties);

        assertAiClientFailure(() -> client.getVerse(16L), FailureCode.TIMEOUT, "bible", "request timed out");
    }

    @Test
    void httpModeRequiresServiceTokenAndBaseUrl() {
        AiClientProperties missingToken = properties();
        missingToken.setServiceToken("");

        assertThatThrownBy(() -> new BibleVerseClientHttpAdapter(objectMapper, missingToken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("service-token");

        AiClientProperties missingBaseUrl = properties();
        missingBaseUrl.getBible().setBaseUrl("");

        assertThatThrownBy(() -> new BibleVerseClientHttpAdapter(objectMapper, missingBaseUrl))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bible base-url");
    }

    private AiClientProperties properties() {
        AiClientProperties properties = new AiClientProperties();
        properties.setMode(AiClientProperties.Mode.HTTP);
        properties.setServiceToken(SERVICE_TOKEN);
        properties.setTimeoutMs(1000);
        String baseUrl = server.url("/").toString();
        properties.getQt().setBaseUrl(baseUrl);
        properties.getBible().setBaseUrl(baseUrl);
        properties.getStudy().setBaseUrl(baseUrl);
        properties.getAudit().setBaseUrl(baseUrl);
        properties.getAdminAuth().setBaseUrl(baseUrl);
        return properties;
    }

    private Map<String, Object> verse(Long verseId, int verse) {
        return Map.of(
                "verseId", verseId,
                "bibleBook", "JOHN",
                "chapter", 3,
                "verse", verse,
                "reference", "JOHN 3:" + verse,
                "koreanText", "Allowed Korean test verse",
                "englishText", "Allowed test verse"
        );
    }

    private Map<String, Object> adminResult(AdminAuthClient.AdminRole role) {
        return Map.of(
                "adminUserId", 1L,
                "memberId", 10L,
                "adminRole", role.name()
        );
    }

    private MockResponse successResponse(Object data) {
        return jsonResponse(200, envelopeBody(true, data, null));
    }

    private MockResponse voidSuccessResponse() {
        return jsonResponse(200, envelopeBody(true, null, null));
    }

    private MockResponse errorResponse(String code, String message, int status) {
        Map<String, Object> error = new LinkedHashMap<>();
        error.put("code", code);
        error.put("message", message);
        error.put("fields", Map.of("reason", "contract-test"));
        return jsonResponse(status, envelopeBody(false, null, error));
    }

    private MockResponse statusResponse(int status) {
        return jsonResponse(status, "{\"message\":\"status failure\"}");
    }

    private MockResponse jsonResponse(int status, String body) {
        return new MockResponse()
                .setResponseCode(status)
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }

    private String envelopeBody(boolean success, Object data, Object error) {
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("success", success);
        envelope.put("data", data);
        envelope.put("error", error);
        envelope.put("timestamp", "2026-06-08T00:00:00+09:00");
        envelope.put("traceId", "trace-contract-test");
        try {
            return objectMapper.writeValueAsString(envelope);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private JsonNode bodyJson(RecordedRequest request) throws IOException {
        return objectMapper.readTree(request.getBody().readUtf8());
    }

    private static void assertCommonHeaders(RecordedRequest request) {
        assertThat(request.getHeader("Authorization")).isEqualTo("Bearer " + SERVICE_TOKEN);
        assertThat(request.getHeader("Accept")).contains("application/json");
    }

    private static void assertIdempotencyKey(RecordedRequest request) {
        assertThat(request.getHeader("Idempotency-Key"))
                .isNotBlank()
                .matches("[0-9a-fA-F-]{36}");
    }

    private void assertAiClientFailure(
            ThrowingRunnable runnable,
            FailureCode expectedCode,
            String expectedDownstream,
            String messageFragment
    ) {
        assertThatThrownBy(runnable::run)
                .isInstanceOf(AiClientException.class)
                .satisfies(exception -> {
                    AiClientException aiClientException = (AiClientException) exception;
                    assertThat(aiClientException.failureCode()).isEqualTo(expectedCode);
                    assertThat(aiClientException.downstreamService()).isEqualTo(expectedDownstream);
                    if (messageFragment != null) {
                        assertThat(aiClientException.getMessage()).contains(messageFragment);
                    }
                });
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
