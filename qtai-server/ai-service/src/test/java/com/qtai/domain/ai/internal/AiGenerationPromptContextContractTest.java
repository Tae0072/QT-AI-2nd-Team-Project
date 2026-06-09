package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerJob;
import com.qtai.domain.ai.internal.AiGenerationWorkerExecutor.AiGenerationWorkerResult;

class AiGenerationPromptContextContractTest {

    private static final String FIXTURE_PATH = "/contracts/ai-generation/prompt-context-contract-fixtures.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void promptContextFixtureDefinesWorkerJobAndPromptMetadataOnly() {
        JsonNode fixture = fixture();
        JsonNode job = fixture.path("job");

        AiGenerationWorkerJob workerJob = new AiGenerationWorkerJob(
                job.path("jobId").longValue(),
                AiGenerationJobType.valueOf(job.path("jobType").asText()),
                AiTargetType.valueOf(job.path("targetType").asText()),
                job.path("targetId").longValue(),
                job.path("promptVersionId").longValue(),
                OffsetDateTime.parse(job.path("startedAt").asText())
        );

        assertThat(workerJob.jobId()).isEqualTo(1001L);
        assertThat(workerJob.jobType()).isEqualTo(AiGenerationJobType.EXPLANATION);
        assertThat(workerJob.targetType()).isEqualTo(AiTargetType.QT_PASSAGE);
        assertThat(workerJob.targetId()).isEqualTo(35L);
        assertThat(workerJob.promptVersionId()).isEqualTo(7L);

        JsonNode promptVersion = fixture.path("promptVersion");
        assertThat(promptVersion.path("promptVersionId").longValue()).isEqualTo(7L);
        assertThat(promptVersion.path("promptType").asText()).isEqualTo("EXPLANATION");
        assertThat(promptVersion.path("version").asText()).isNotBlank();
        assertThat(promptVersion.path("contentHash").asText()).startsWith("sha256:");
        assertMissingFields(promptVersion, "content", "body", "prompt", "promptText", "prompt_text");
    }

    @Test
    void providerContextFixtureUsesAllowedMetadataContextAndReferenceOnlyBibleFields() {
        JsonNode providerContext = fixture().path("providerContext");
        JsonNode qtContext = providerContext.path("qtContext");

        assertThat(qtContext.path("passageId").longValue()).isEqualTo(35L);
        assertThat(qtContext.path("passageReference").asText()).isEqualTo("John 3:16-17");
        assertThat(qtContext.path("passageContext").asText()).contains("ALLOWED_METADATA_CONTEXT_BLOCK");
        assertMissingFields(qtContext, "cacheStatus");

        JsonNode bibleVerseRefs = providerContext.path("bibleVerseRefs");
        assertThat(bibleVerseRefs).hasSize(2);
        for (JsonNode bibleVerseRef : bibleVerseRefs) {
            assertThat(bibleVerseRef.path("verseId").isNumber()).isTrue();
            assertThat(bibleVerseRef.path("reference").asText()).isNotBlank();
            assertMissingFields(
                    bibleVerseRef,
                    "koreanText",
                    "korean_text",
                    "englishText",
                    "english_text",
                    "bibleText",
                    "bible_text",
                    "scriptureText",
                    "scripture_text"
            );
        }
    }

    @Test
    void expectedResultPayloadMatchesWorkerResultContract() throws Exception {
        JsonNode expectedResult = fixture().path("expectedResult");
        String payloadJson = OBJECT_MAPPER.writeValueAsString(expectedResult.path("payloadJson"));

        AiGenerationWorkerResult result = AiGenerationWorkerResult.of(
                AiGeneratedAssetType.valueOf(expectedResult.path("assetType").asText()),
                payloadJson,
                expectedResult.path("sourceLabel").asText()
        );

        assertThat(result.assetType()).isEqualTo(AiGeneratedAssetType.EXPLANATION);
        assertThat(result.sourceLabel()).isEqualTo("AI-WORKER-CONTRACT");
        assertThat(result.payloadJson()).contains("Allowed generated summary");
        assertMissingFields(
                expectedResult.path("payloadJson"),
                "prompt",
                "promptText",
                "providerRawResponse",
                "rawResponse",
                "referenceText",
                "scriptureText",
                "bibleText",
                "credentialValue",
                "authHeaderValue",
                "dbConnectionValue"
        );
    }

    @Test
    void forbiddenPayloadExamplesAreRejectedByWorkerResultContract() {
        JsonNode examples = fixture().path("forbiddenPayloadExamples");

        assertThat(examples).hasSizeGreaterThanOrEqualTo(4);
        for (JsonNode example : examples) {
            String payloadJson = uncheckedWrite(example.path("payloadJson"));

            assertThatThrownBy(() -> AiGenerationWorkerResult.of(
                    AiGeneratedAssetType.EXPLANATION,
                    payloadJson,
                    "AI-WORKER-CONTRACT"
            )).as(example.path("name").asText())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("payloadJson must not store forbidden provider or validation reference fields");
        }
    }

    private static JsonNode fixture() {
        try (InputStream inputStream = AiGenerationPromptContextContractTest.class.getResourceAsStream(FIXTURE_PATH)) {
            assertThat(inputStream).as("fixture resource must exist").isNotNull();
            return OBJECT_MAPPER.readTree(inputStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load prompt context contract fixture", exception);
        }
    }

    private static void assertMissingFields(JsonNode node, String... fieldNames) {
        List<String> presentFields = List.of(fieldNames).stream()
                .filter(fieldName -> !node.findPath(fieldName).isMissingNode())
                .toList();

        assertThat(presentFields).isEmpty();
    }

    private static String uncheckedWrite(JsonNode node) {
        try {
            return OBJECT_MAPPER.writeValueAsString(node);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to serialize fixture node", exception);
        }
    }
}
