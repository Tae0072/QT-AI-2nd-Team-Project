package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.external.llm.LlmClient;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

class ExplanationGenerationJobHandlerTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-29T04:02:00+09:00");

    private GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private GetBibleVerseUseCase getBibleVerseUseCase;
    private AiPromptVersionRepository promptVersionRepository;
    private LlmClient llmClient;
    private ObjectMapper objectMapper;
    private ExplanationGenerationJobHandler handler;

    @BeforeEach
    void setUp() {
        getQtPassageContentContextUseCase = mock(GetQtPassageContentContextUseCase.class);
        getBibleVerseUseCase = mock(GetBibleVerseUseCase.class);
        promptVersionRepository = mock(AiPromptVersionRepository.class);
        llmClient = mock(LlmClient.class);
        objectMapper = new ObjectMapper();
        handler = new ExplanationGenerationJobHandler(
                getQtPassageContentContextUseCase,
                getBibleVerseUseCase,
                promptVersionRepository,
                llmClient,
                objectMapper
        );
    }

    @Test
    void createsVerseBasedPayloadFromQtAndBibleContext() throws Exception {
        AiGenerationJob job = job();
        givenPromptVersion();
        givenQtContext(List.of(1001L, 1002L));
        givenBibleVerses(List.of(1001L, 1002L));
        when(llmClient.complete(any())).thenReturn(completionResponse("""
                {
                  "explanations": [
                    {"verseId": 1001, "summary": "summary one", "explanation": "explanation one"},
                    {"verseId": 1002, "summary": "summary two", "explanation": "explanation two"}
                  ],
                  "glossaryTerms": [
                    {"verseId": 1001, "term": "context", "meaning": "background meaning"}
                  ]
                }
                """));

        AiGeneratedAsset asset = handler.generate(job, CREATED_AT);

        assertThat(asset.getGenerationJobId()).isEqualTo(900L);
        assertThat(asset.getAssetType()).isEqualTo(AiGeneratedAssetType.EXPLANATION);
        assertThat(asset.getTargetType()).isEqualTo(AiTargetType.QT_PASSAGE);
        assertThat(asset.getTargetId()).isEqualTo(35L);

        JsonNode payload = objectMapper.readTree(asset.getPayloadJson());
        assertThat(payload.path("explanations")).hasSize(2);
        assertThat(payload.path("explanations").get(0).path("verseId").asLong()).isEqualTo(1001L);
        assertThat(payload.path("glossaryTerms")).hasSize(1);
        assertThat(payload.path("glossaryTerms").get(0).path("verseId").asLong()).isEqualTo(1001L);
        assertThat(payload.path("promptVersionId").asLong()).isEqualTo(3L);
        assertThat(payload.path("promptVersion").asText()).isEqualTo("2026.05.1");
        assertThat(payload.path("promptContentHash").asText()).isEqualTo("hash-3");
        assertThat(payload.path("modelName").asText()).isEqualTo("deepseek-test");
        assertThat(payload.path("tokenUsage").path("totalTokens").asInt()).isEqualTo(33);
        assertThat(payload.path("sourceMetadata").path("verseIds")).hasSize(2);
        assertThat(payload.path("sourceMetadata").path("verses").get(0).has("koreanText")).isFalse();

        ArgumentCaptor<LlmCompletionRequest> requestCaptor = ArgumentCaptor.forClass(LlmCompletionRequest.class);
        verify(llmClient).complete(requestCaptor.capture());
        assertThat(requestCaptor.getValue().prompt())
                .contains("verseId=1001", "verseId=1002", "QT title");
    }

    @Test
    void payloadDoesNotStoreProviderRawPromptOrReferenceFields() throws Exception {
        AiGenerationJob job = job();
        givenPromptVersion();
        givenQtContext(List.of(1001L));
        givenBibleVerses(List.of(1001L));
        when(llmClient.complete(any())).thenReturn(completionResponse("""
                {
                  "providerRawResponse": "raw model payload",
                  "validationReferenceText": "reference source text",
                  "promptText": "full prompt",
                  "explanations": [
                    {"verseId": 1001, "summary": "summary one", "explanation": "explanation one"}
                  ],
                  "glossaryTerms": []
                }
                """));

        AiGeneratedAsset asset = handler.generate(job, CREATED_AT);
        JsonNode payload = objectMapper.readTree(asset.getPayloadJson());

        assertThat(payload.has("providerRawResponse")).isFalse();
        assertThat(payload.has("validationReferenceText")).isFalse();
        assertThat(payload.has("promptText")).isFalse();
        assertThat(asset.getPayloadJson()).doesNotContain(
                "raw model payload",
                "reference source text",
                "full prompt"
        );
    }

    @Test
    void invalidProviderJsonIsRejected() {
        AiGenerationJob job = job();
        givenPromptVersion();
        givenQtContext(List.of(1001L));
        givenBibleVerses(List.of(1001L));
        when(llmClient.complete(any())).thenReturn(completionResponse("{"));

        assertThatThrownBy(() -> handler.generate(job, CREATED_AT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    void outOfScopeVerseIdIsRejected() {
        AiGenerationJob job = job();
        givenPromptVersion();
        givenQtContext(List.of(1001L));
        givenBibleVerses(List.of(1001L));
        when(llmClient.complete(any())).thenReturn(completionResponse("""
                {
                  "explanations": [
                    {"verseId": 9999, "summary": "summary", "explanation": "explanation"}
                  ],
                  "glossaryTerms": []
                }
                """));

        assertThatThrownBy(() -> handler.generate(job, CREATED_AT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    private void givenPromptVersion() {
        when(promptVersionRepository.findById(3L)).thenReturn(Optional.of(AiPromptVersion.of(
                3L,
                AiPromptType.EXPLANATION,
                "2026.05.1",
                "hash-3",
                AiPromptVersionStatus.ACTIVE,
                CREATED_AT.minusDays(1)
        )));
    }

    private void givenQtContext(List<Long> verseIds) {
        when(getQtPassageContentContextUseCase.getContentContext(35L))
                .thenReturn(new QtPassageContentContext(
                        35L,
                        LocalDate.of(2026, 5, 29),
                        "QT title",
                        verseIds,
                        true
                ));
    }

    private void givenBibleVerses(List<Long> verseIds) {
        when(getBibleVerseUseCase.getVerses(verseIds))
                .thenReturn(verseIds.stream()
                        .map(verseId -> new BibleVerseResponse(
                                verseId,
                                "TST",
                                1,
                                verseId.intValue(),
                                "neutral text " + verseId,
                                null
                        ))
                        .toList());
    }

    private static LlmCompletionResponse completionResponse(String content) {
        return new LlmCompletionResponse(
                content,
                11,
                22,
                33,
                "deepseek-test"
        );
    }

    private static AiGenerationJob job() {
        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                CREATED_AT.minusMinutes(2)
        );
        setId(job, 900L);
        return job;
    }

    private static void setId(Object target, Long id) {
        try {
            Field idField = target.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(target, id);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
