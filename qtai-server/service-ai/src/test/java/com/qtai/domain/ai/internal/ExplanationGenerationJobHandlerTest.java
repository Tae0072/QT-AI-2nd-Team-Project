package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.external.llm.LlmClient;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ExplanationGenerationJobHandlerTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-11T09:00:00+09:00");

    private final GetQtPassageContentContextUseCase qtContextUseCase =
            mock(GetQtPassageContentContextUseCase.class);
    private final GetBibleVerseUseCase bibleVerseUseCase = mock(GetBibleVerseUseCase.class);
    private final CommentaryMaterialService commentaryMaterialService = mock(CommentaryMaterialService.class);
    private final AiPromptVersionRepository promptVersionRepository = mock(AiPromptVersionRepository.class);
    private final LlmClient llmClient = mock(LlmClient.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ExplanationGenerationJobHandler handler = new ExplanationGenerationJobHandler(
            qtContextUseCase,
            bibleVerseUseCase,
            commentaryMaterialService,
            promptVersionRepository,
            llmClient,
            objectMapper
    );

    @Test
    void generateAddsEmptyCommentaryMetadataWhenNoMaterials() throws Exception {
        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                12L,
                CREATED_AT
        );
        setId(job, 7002L);

        when(promptVersionRepository.findById(12L)).thenReturn(Optional.of(AiPromptVersion.of(
                12L,
                AiPromptType.EXPLANATION,
                "2026.06.2",
                "hash-002",
                AiPromptVersionStatus.ACTIVE,
                "custom system prompt",
                "Custom target {{targetId}}\n{{versesBlock}}{{commentaryBlock}}",
                "deepseek-reasoner",
                0.4,
                1234,
                "custom prompt",
                2L,
                CREATED_AT.minusDays(1),
                CREATED_AT.minusDays(1),
                null
        )));
        when(bibleVerseUseCase.getVerses(List.of(1001L))).thenReturn(List.of(new BibleVerseResponse(
                1001L,
                "Gen",
                1,
                1,
                "test verse",
                "test verse"
        )));
        when(commentaryMaterialService.findPromptContextByVerseIds(List.of(1001L)))
                .thenReturn(CommentaryMaterialContext.empty());
        when(llmClient.complete(any(LlmCompletionRequest.class))).thenReturn(completionResponse());

        AiGeneratedAsset asset = handler.generate(job, CREATED_AT);

        ArgumentCaptor<LlmCompletionRequest> requestCaptor = ArgumentCaptor.forClass(LlmCompletionRequest.class);
        org.mockito.Mockito.verify(llmClient).complete(requestCaptor.capture());
        assertThat(requestCaptor.getValue().model()).isEqualTo("deepseek-reasoner");
        assertThat(requestCaptor.getValue().systemPrompt()).isEqualTo("custom system prompt");
        assertThat(requestCaptor.getValue().maxTokens()).isEqualTo(1234);
        assertThat(requestCaptor.getValue().temperature()).isEqualTo(0.4);
        assertThat(requestCaptor.getValue().prompt())
                .contains("Custom target 1001")
                .doesNotContain("Commentary materials:")
                .doesNotContain("Commentary excerpt");

        JsonNode sourceMetadata = objectMapper.readTree(asset.getPayloadJson()).get("sourceMetadata");
        assertEmptyCommentaryMetadata(sourceMetadata);
    }

    @Test
    void generateAddsCommentaryExcerptToPromptAndSourceMetadata() throws Exception {
        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                9001L,
                11L,
                CREATED_AT
        );
        setId(job, 7001L);

        when(promptVersionRepository.findById(11L)).thenReturn(Optional.of(AiPromptVersion.of(
                11L,
                AiPromptType.EXPLANATION,
                "2026.06.1",
                "hash-001",
                AiPromptVersionStatus.ACTIVE,
                CREATED_AT.minusDays(1)
        )));
        when(qtContextUseCase.getContentContext(9001L)).thenReturn(new QtPassageContentContext(
                9001L,
                LocalDate.of(2026, 6, 11),
                "QT title",
                List.of(1001L),
                true
        ));
        when(bibleVerseUseCase.getVerses(List.of(1001L))).thenReturn(List.of(new BibleVerseResponse(
                1001L,
                "Gen",
                1,
                1,
                "테스트 본문",
                "test verse"
        )));
        when(commentaryMaterialService.findPromptContextByVerseIds(List.of(1001L)))
                .thenReturn(new CommentaryMaterialContext(
                        "TYNDALE_OPEN_STUDY_NOTES",
                        "Tyndale Open Study Notes",
                        "CC BY-SA 4.0",
                        "Copyright notice",
                        List.of(3001L),
                        "Gen.1.1-1.1",
                        List.of(new CommentaryMaterialContext.MaterialExcerpt(
                                3001L,
                                "Gen.1.1",
                                "Creation",
                                "Commentary excerpt",
                                List.of(1001L)
                        ))
                ));
        when(llmClient.complete(any(LlmCompletionRequest.class))).thenReturn(completionResponse());

        AiGeneratedAsset asset = handler.generate(job, CREATED_AT);

        ArgumentCaptor<LlmCompletionRequest> requestCaptor = ArgumentCaptor.forClass(LlmCompletionRequest.class);
        org.mockito.Mockito.verify(llmClient).complete(requestCaptor.capture());
        assertThat(requestCaptor.getValue().prompt())
                .contains("Commentary materials:")
                .contains("Tyndale Open Study Notes")
                .contains("Commentary excerpt")
                .contains("materialId=3001");

        JsonNode sourceMetadata = objectMapper.readTree(asset.getPayloadJson()).get("sourceMetadata");
        assertThat(sourceMetadata.get("commentarySource").asText()).isEqualTo("TYNDALE_OPEN_STUDY_NOTES");
        assertThat(sourceMetadata.get("sourceName").asText()).isEqualTo("Tyndale Open Study Notes");
        assertThat(sourceMetadata.get("licenseLabel").asText()).isEqualTo("CC BY-SA 4.0");
        assertThat(sourceMetadata.get("copyrightNotice").asText()).isEqualTo("Copyright notice");
        assertThat(sourceMetadata.get("commentaryMaterialIds").get(0).asLong()).isEqualTo(3001L);
        assertThat(sourceMetadata.get("commentaryVerseRange").asText()).isEqualTo("Gen.1.1-1.1");
    }

    private static LlmCompletionResponse completionResponse() {
        return new LlmCompletionResponse(
                """
                        {
                          "explanations": [
                            {
                              "verseId": 1001,
                              "summary": "summary",
                              "explanation": "explanation"
                            }
                          ],
                          "glossaryTerms": [
                            {
                              "verseId": 1001,
                              "term": "term",
                              "meaning": "meaning"
                            }
                          ]
                        }
                        """,
                10,
                20,
                30,
                "deepseek-chat"
        );
    }

    private static void assertEmptyCommentaryMetadata(JsonNode sourceMetadata) {
        assertThat(sourceMetadata.get("commentarySource").isNull()).isTrue();
        assertThat(sourceMetadata.get("sourceName").isNull()).isTrue();
        assertThat(sourceMetadata.get("licenseLabel").isNull()).isTrue();
        assertThat(sourceMetadata.get("copyrightNotice").isNull()).isTrue();
        assertThat(sourceMetadata.get("commentaryVerseRange").isNull()).isTrue();
        assertThat(sourceMetadata.get("commentaryMaterialIds").isArray()).isTrue();
        assertThat(sourceMetadata.get("commentaryMaterialIds").isEmpty()).isTrue();
    }

    private static void setId(Object target, Long id) throws ReflectiveOperationException {
        Field idField = target.getClass().getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(target, id);
    }
}
