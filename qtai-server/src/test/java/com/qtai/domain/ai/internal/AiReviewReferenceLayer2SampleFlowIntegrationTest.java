package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.qtai.external.llm.LlmClient;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

@SpringBootTest
@ActiveProfiles("test")
class AiReviewReferenceLayer2SampleFlowIntegrationTest {

    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-06-05T10:00:00+09:00");
    private static final OffsetDateTime VALIDATED_AT = OffsetDateTime.parse("2026-06-05T10:05:00+09:00");
    private static final Path RESTRICTED_ROOT = createRestrictedRoot();
    private static final String SOURCE_FILE_HASH = "sha256:synthetic-layer2-reference-hash";
    private static final String INDEX_STORAGE_URI = "restricted://validation/index/reference-index.json";
    private static final String MATCHING_REFERENCE_TEXT = "synthetic-layer2-reference matching prompt only text";
    private static final String NON_MATCHING_REFERENCE_TEXT = "synthetic-layer2-reference non matching prompt only text";

    @Autowired
    private AiReviewValidationService reviewValidationService;

    @Autowired
    private AiGeneratedAssetRepository generatedAssetRepository;

    @Autowired
    private AiValidationChecklistVersionRepository checklistVersionRepository;

    @Autowired
    private AiValidationLogRepository validationLogRepository;

    @Autowired
    private ValidationReferenceJobRepository validationReferenceJobRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private LlmClient llmClient;

    @DynamicPropertySource
    static void restrictedStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("qtai.validation.restricted-storage-root", () -> RESTRICTED_ROOT.toString());
    }

    @BeforeEach
    void setUp() throws Exception {
        clearAiValidationData();
        writeSyntheticIndex();
    }

    @AfterEach
    void tearDown() {
        clearAiValidationData();
    }

    @AfterAll
    static void tearDownRestrictedRoot() throws IOException {
        deleteRecursively(RESTRICTED_ROOT);
    }

    @Test
    void layer2ValidationReadsRestrictedIndexInjectsMatchingExcerptAndStoresOnlyMetadata() throws Exception {
        ValidationReferenceJob referenceJob = saveActiveReferenceJob();
        AiValidationChecklistVersion checklistVersion = saveActiveExplanationChecklist();
        AiGeneratedAsset asset = saveExplanationAsset();
        when(llmClient.complete(any(LlmCompletionRequest.class)))
                .thenReturn(new LlmCompletionResponse("{\"result\":\"PASSED\"}", 120, 12, 132, "fake-layer2"));

        AiValidationLog log = reviewValidationService.validateExplanationAsset(asset.getId(), VALIDATED_AT);

        assertThat(log.getLayer()).isEqualTo(2);
        assertThat(log.getReviewerType()).isEqualTo(AiValidationReviewerType.ADVISOR);
        assertThat(log.getResult()).isEqualTo(AiValidationResult.PASSED);
        assertThat(log.getValidationReferenceJobId()).isEqualTo(referenceJob.getId());
        assertThat(log.getChecklistVersionId()).isEqualTo(checklistVersion.getId());
        assertThat(log.getErrorMessage()).isNull();

        ArgumentCaptor<LlmCompletionRequest> requestCaptor = ArgumentCaptor.forClass(LlmCompletionRequest.class);
        verify(llmClient).complete(requestCaptor.capture());
        String promptJson = requestCaptor.getValue().prompt();
        JsonNode prompt = objectMapper.readTree(promptJson);
        JsonNode reference = prompt.path("reference");
        JsonNode excerpts = reference.path("excerpts");

        assertThat(reference.path("validationReferenceJobId").asLong()).isEqualTo(referenceJob.getId());
        assertThat(reference.path("sourceFileHash").asText()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(reference.path("indexStorageUri").asText()).isEqualTo(INDEX_STORAGE_URI);
        assertThat(excerpts.isArray()).isTrue();
        assertThat(excerpts.size()).isEqualTo(1);
        assertThat(excerpts.get(0).path("bookCode").asText()).isEqualTo("JHN");
        assertThat(excerpts.get(0).path("chapterStart").asInt()).isEqualTo(3);
        assertThat(excerpts.get(0).path("verseStart").asInt()).isEqualTo(16);
        assertThat(excerpts.get(0).path("chapterEnd").asInt()).isEqualTo(3);
        assertThat(excerpts.get(0).path("verseEnd").asInt()).isEqualTo(18);
        assertThat(excerpts.get(0).path("referenceHash").asText()).isEqualTo("sha256:synthetic-layer2-jhn");
        assertThat(excerpts.get(0).path("referenceText").asText()).isEqualTo(MATCHING_REFERENCE_TEXT);
        assertThat(promptJson)
                .contains(MATCHING_REFERENCE_TEXT)
                .doesNotContain(NON_MATCHING_REFERENCE_TEXT);

        AiValidationLog storedLog = validationLogRepository.findById(log.getId()).orElseThrow();
        JsonNode checklistJson = objectMapper.readTree(storedLog.getChecklistJson());
        assertThat(checklistJson.path("validationReferenceJobId").asLong()).isEqualTo(referenceJob.getId());
        assertThat(checklistJson.path("referenceSourceFileHash").asText()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(checklistJson.path("referenceIndexStorageUri").asText()).isEqualTo(INDEX_STORAGE_URI);
        assertThat(checklistJson.path("selectedReferenceExcerptCount").asInt()).isEqualTo(1);
        assertThat(checklistJson.path("selectedReferenceHashes").get(0).asText())
                .isEqualTo("sha256:synthetic-layer2-jhn");
        assertThat(checklistJson.path("selectedReferenceRangeLabels").get(0).asText())
                .isEqualTo("JHN 3:16-18");
        assertThat(storedLog.getChecklistJson())
                .doesNotContain("referenceText", MATCHING_REFERENCE_TEXT, NON_MATCHING_REFERENCE_TEXT);
    }

    private ValidationReferenceJob saveActiveReferenceJob() {
        return validationReferenceJobRepository.saveAndFlush(ValidationReferenceJob.create(
                "검증 참조 자료",
                "reference-notes.pdf",
                SOURCE_FILE_HASH,
                "restricted://validation/reference.pdf",
                INDEX_STORAGE_URI,
                CREATED_AT.plusDays(7),
                CREATED_AT
        ));
    }

    private AiValidationChecklistVersion saveActiveExplanationChecklist() {
        AiValidationChecklistVersion checklistVersion = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "2026.06.05-layer2-sample",
                "sha256:layer2-sample-checklist",
                null,
                CREATED_AT.minusDays(1)
        );
        checklistVersion.activate(CREATED_AT.minusHours(1));
        return checklistVersionRepository.saveAndFlush(checklistVersion);
    }

    private AiGeneratedAsset saveExplanationAsset() {
        return generatedAssetRepository.saveAndFlush(AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                """
                        {
                          "explanations": [
                            {"verseId": 1001, "summary": "summary", "explanation": "explanation"}
                          ],
                          "sourceMetadata": {
                            "verseIds": [1001],
                            "verses": [
                              {"verseId": 1001, "bookCode": "JHN", "chapterNo": 3, "verseNo": 16}
                            ]
                          }
                        }
                        """,
                "QT-AI DeepSeek",
                CREATED_AT
        ));
    }

    private void writeSyntheticIndex() throws IOException {
        Path indexPath = RESTRICTED_ROOT.resolve("validation").resolve("index").resolve("reference-index.json");
        Files.createDirectories(indexPath.getParent());
        Files.writeString(indexPath, """
                {
                  "schemaVersion": "ai-review-reference-index.v1",
                  "sourceFileHash": "%s",
                  "generatedAt": "2026-06-05T10:00:00+09:00",
                  "entries": [
                    {
                      "bookCode": "JHN",
                      "chapterStart": 3,
                      "verseStart": 16,
                      "chapterEnd": 3,
                      "verseEnd": 18,
                      "referenceRangeLabel": "JHN 3:16-18",
                      "referenceText": "%s",
                      "referenceHash": "sha256:synthetic-layer2-jhn"
                    },
                    {
                      "bookCode": "ROM",
                      "chapterStart": 1,
                      "verseStart": 1,
                      "chapterEnd": 1,
                      "verseEnd": 2,
                      "referenceRangeLabel": "ROM 1:1-2",
                      "referenceText": "%s",
                      "referenceHash": "sha256:synthetic-layer2-rom"
                    }
                  ]
                }
                """.formatted(SOURCE_FILE_HASH, MATCHING_REFERENCE_TEXT, NON_MATCHING_REFERENCE_TEXT),
                StandardCharsets.UTF_8);
    }

    private void clearAiValidationData() {
        validationLogRepository.deleteAll();
        generatedAssetRepository.deleteAll();
        checklistVersionRepository.deleteAll();
        validationReferenceJobRepository.deleteAll();
        validationLogRepository.flush();
        generatedAssetRepository.flush();
        checklistVersionRepository.flush();
        validationReferenceJobRepository.flush();
    }

    private static Path createRestrictedRoot() {
        try {
            return Files.createTempDirectory("qtai-layer2-sample-flow-");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        if (!Files.exists(root)) {
            return;
        }
        try (Stream<Path> paths = Files.walk(root)) {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    throw new UncheckedIOException(exception);
                }
            });
        } catch (UncheckedIOException exception) {
            throw exception.getCause();
        }
    }
}
