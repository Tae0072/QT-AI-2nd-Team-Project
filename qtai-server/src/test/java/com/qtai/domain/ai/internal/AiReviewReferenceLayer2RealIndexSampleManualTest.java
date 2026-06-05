package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.external.llm.DeepSeekLlmClient;
import com.qtai.external.llm.LlmClient;
import com.qtai.external.llm.dto.LlmCompletionRequest;
import com.qtai.external.llm.dto.LlmCompletionResponse;

@SpringBootTest(properties = {
        "spring.main.lazy-initialization=true",
        "spring.datasource.url=${DB_URL:jdbc:mysql://localhost:3306/qtai?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8}",
        "spring.datasource.username=${DB_USERNAME:qtai}",
        "spring.datasource.password=${DB_PASSWORD:qtai}",
        "spring.flyway.enabled=false",
        "ai.generation.worker.enabled=false",
        "external.llm.deepseek.api-key=${DEEPSEEK_API_KEY:}",
        "external.llm.deepseek.base-url=${DEEPSEEK_BASE_URL:https://api.deepseek.com}",
        "external.llm.deepseek.model=${DEEPSEEK_MODEL:deepseek-v4-flash}",
        "external.llm.deepseek.connect-timeout-ms=${DEEPSEEK_CONNECT_TIMEOUT_MS:3000}",
        "external.llm.deepseek.read-timeout-ms=${DEEPSEEK_READ_TIMEOUT_MS:30000}"
})
@ActiveProfiles("dev")
@EnabledIf("realSampleEnabled")
class AiReviewReferenceLayer2RealIndexSampleManualTest {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final String INDEX_STORAGE_URI = "restricted://validation/index/reference-index.json";
    private static final String SOURCE_FILE_HASH =
            "sha256:d50811d18c1d109a1ce0dc8331f25bb7daf249be1892d9ca742cbb64c20eca8b";
    private static final String SAMPLE_BOOK_CODE = "JHN";
    private static final int SAMPLE_CHAPTER = 3;
    private static final int SAMPLE_VERSE = 16;
    private static final long SAMPLE_TARGET_ID = 990_003_016L;
    private static final String EXPECTED_REFERENCE_HASH_1 =
            "sha256:f090c1fc624266a2fc1a8842aad1eafdc7c43f36dd8dfe904e81321882220a18";
    private static final String EXPECTED_REFERENCE_HASH_2 =
            "sha256:559619a06e4046fcf55d95653e9a64260475af38ac13324094f501b264f42926";

    @Autowired
    private AiReviewValidationService reviewValidationService;

    @Autowired
    private AiReviewReferenceIndexReader referenceIndexReader;

    @Autowired
    private ValidationReferenceJobRepository validationReferenceJobRepository;

    @Autowired
    private AiValidationChecklistVersionRepository checklistVersionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AiGenerationJobRepository generationJobRepository;

    @Autowired
    private AiGeneratedAssetRepository generatedAssetRepository;

    @Autowired
    private AiValidationLogRepository validationLogRepository;

    @Autowired
    private CapturingLlmClient capturingLlmClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${external.llm.deepseek.api-key:}")
    private String deepSeekApiKey;

    @DynamicPropertySource
    static void restrictedStorageProperties(DynamicPropertyRegistry registry) {
        registry.add("qtai.validation.restricted-storage-root", () ->
                projectDir().resolve("restricted").toAbsolutePath().normalize().toString());
    }

    @Test
    @Transactional
    void layer2RealIndexSampleRunReadsRestrictedIndexCallsDeepSeekAndStoresOnlyMetadata() throws Exception {
        assertThat(deepSeekApiKey).isNotBlank();
        assertThat(realIndexPath()).isRegularFile();

        AiReviewReferenceIndexReader.ReferenceIndex index =
                referenceIndexReader.read(INDEX_STORAGE_URI, SOURCE_FILE_HASH);
        assertThat(index.schemaVersion()).isEqualTo("ai-review-reference-index.v1");
        assertThat(index.sourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(index.entries()).hasSize(3_021);

        ValidationReferenceJob referenceJob = saveSampleReferenceJob();
        AiValidationChecklistVersion checklistVersion = ensureSingleActiveExplanationChecklist();
        AiGenerationJob generationJob = saveSampleGenerationJob();
        AiGeneratedAsset asset = saveSampleAsset(generationJob);

        AiValidationLog log = reviewValidationService.validateExplanationAsset(asset.getId(), now());

        assertThat(capturingLlmClient.callCount()).isEqualTo(1);
        assertThat(log.getLayer()).isEqualTo(2);
        assertThat(log.getReviewerType()).isEqualTo(AiValidationReviewerType.ADVISOR);
        assertThat(log.getValidationReferenceJobId()).isEqualTo(referenceJob.getId());
        assertThat(log.getChecklistVersionId()).isEqualTo(checklistVersion.getId());
        assertThat(log.getResult()).isIn(
                AiValidationResult.PASSED,
                AiValidationResult.REJECTED,
                AiValidationResult.NEEDS_REVIEW
        );

        PromptReferenceSnapshot promptSnapshot = promptReferenceSnapshot(capturingLlmClient.lastRequest());
        assertThat(promptSnapshot.indexStorageUri()).isEqualTo(INDEX_STORAGE_URI);
        assertThat(promptSnapshot.sourceFileHash()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(promptSnapshot.selectedCount()).isGreaterThanOrEqualTo(1);
        assertThat(promptSnapshot.selectedHashes())
                .contains(EXPECTED_REFERENCE_HASH_1, EXPECTED_REFERENCE_HASH_2);
        assertThat(promptSnapshot.selectedRangeLabels())
                .anyMatch(range -> range.contains("3:9-21"))
                .anyMatch(range -> range.contains("3:14-30"));
        assertThat(promptSnapshot.selectedTexts().size()).isEqualTo(promptSnapshot.selectedCount());

        AiValidationLog storedLog = validationLogRepository.findById(log.getId()).orElseThrow();
        String checklistJson = storedLog.getChecklistJson();
        JsonNode checklist = objectMapper.readTree(checklistJson);
        assertThat(checklist.path("referenceIndexStorageUri").asText()).isEqualTo(INDEX_STORAGE_URI);
        assertThat(checklist.path("referenceSourceFileHash").asText()).isEqualTo(SOURCE_FILE_HASH);
        assertThat(checklist.path("selectedReferenceExcerptCount").asInt())
                .isEqualTo(promptSnapshot.selectedCount());
        assertThat(checklistJson).doesNotContain("referenceText");
        boolean sourceTextStored = promptSnapshot.selectedTexts().stream().anyMatch(checklistJson::contains);
        assertThat(sourceTextStored).isFalse();

        writeSummary(index, referenceJob, asset, storedLog, promptSnapshot);
    }

    private ValidationReferenceJob saveSampleReferenceJob() {
        return validationReferenceJobRepository.saveAndFlush(ValidationReferenceJob.create(
                "IVP reference real sample",
                "manual-reference.pdf",
                SOURCE_FILE_HASH,
                "restricted://validation/reference.pdf",
                INDEX_STORAGE_URI,
                now().plusDays(7),
                now()
        ));
    }

    private AiValidationChecklistVersion ensureSingleActiveExplanationChecklist() {
        List<AiValidationChecklistVersion> activeVersions = checklistVersionRepository.findByChecklistTypeAndStatus(
                AiValidationChecklistType.EXPLANATION,
                AiValidationChecklistStatus.ACTIVE
        );
        assertThat(activeVersions).hasSizeLessThanOrEqualTo(1);
        if (!activeVersions.isEmpty()) {
            return activeVersions.get(0);
        }

        AiValidationChecklistVersion checklistVersion = AiValidationChecklistVersion.create(
                AiValidationChecklistType.EXPLANATION,
                "real-" + System.currentTimeMillis(),
                "sha256:layer2-real-index-sample-checklist",
                null,
                now().minusDays(1)
        );
        checklistVersion.activate(now().minusHours(1));
        return checklistVersionRepository.saveAndFlush(checklistVersion);
    }

    private AiGenerationJob saveSampleGenerationJob() {
        Long promptVersionId = insertSamplePromptVersion();
        return generationJobRepository.saveAndFlush(AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                SAMPLE_TARGET_ID + Math.floorMod(System.nanoTime(), 10_000L),
                promptVersionId,
                now().minusMinutes(10)
        ));
    }

    private Long insertSamplePromptVersion() {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    insert into ai_prompt_versions
                        (prompt_type, version, content_hash, status, created_at)
                    values (?, ?, ?, ?, ?)
                    """, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, AiPromptType.EXPLANATION.name());
            statement.setString(2, "real-" + System.currentTimeMillis());
            statement.setString(3, "sha256:layer2-real-index-sample-prompt");
            statement.setString(4, AiPromptVersionStatus.ACTIVE.name());
            statement.setObject(5, now().minusDays(1).toLocalDateTime());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        assertThat(key).isNotNull();
        return key.longValue();
    }

    private AiGeneratedAsset saveSampleAsset(AiGenerationJob generationJob) {
        return generatedAssetRepository.saveAndFlush(AiGeneratedAsset.create(
                generationJob.getId(),
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                generationJob.getTargetId(),
                """
                        {
                          "explanations": [
                            {
                              "verseId": 300316,
                              "summary": "manual sample summary",
                              "explanation": "manual sample explanation for layer 2 validation"
                            }
                          ],
                          "sourceMetadata": {
                            "verseIds": [300316],
                            "verses": [
                              {
                                "verseId": 300316,
                                "bookCode": "JHN",
                                "chapterNo": 3,
                                "verseNo": 16
                              }
                            ]
                          }
                        }
                        """,
                "QT-AI manual sample",
                now().minusMinutes(5)
        ));
    }

    private PromptReferenceSnapshot promptReferenceSnapshot(LlmCompletionRequest request) throws IOException {
        assertThat(request).isNotNull();
        JsonNode prompt = objectMapper.readTree(request.prompt());
        JsonNode reference = prompt.path("reference");
        JsonNode excerpts = reference.path("excerpts");
        assertThat(excerpts.isArray()).isTrue();

        List<String> hashes = new ArrayList<>();
        List<String> ranges = new ArrayList<>();
        List<String> selectedTexts = new ArrayList<>();
        for (JsonNode excerpt : excerpts) {
            hashes.add(excerpt.path("referenceHash").asText());
            ranges.add(excerpt.path("referenceRangeLabel").asText());
            String selectedText = excerpt.path("referenceText").asText();
            if (!selectedText.isBlank()) {
                selectedTexts.add(selectedText);
            }
        }
        return new PromptReferenceSnapshot(
                reference.path("sourceFileHash").asText(),
                reference.path("indexStorageUri").asText(),
                excerpts.size(),
                hashes,
                ranges,
                selectedTexts
        );
    }

    private void writeSummary(
            AiReviewReferenceIndexReader.ReferenceIndex index,
            ValidationReferenceJob referenceJob,
            AiGeneratedAsset asset,
            AiValidationLog log,
            PromptReferenceSnapshot promptSnapshot
    ) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", "ai-review-layer2-real-index-sample-summary.v1");
        root.put("referenceIndexSchemaVersion", index.schemaVersion());
        root.put("sourceFileHash", index.sourceFileHash());
        root.put("entryCount", index.entries().size());
        root.put("sampleBookCode", SAMPLE_BOOK_CODE);
        root.put("sampleChapter", SAMPLE_CHAPTER);
        root.put("sampleVerse", SAMPLE_VERSE);
        root.put("validationReferenceJobId", referenceJob.getId());
        root.put("assetId", asset.getId());
        root.put("validationLogId", log.getId());
        root.put("layer2Result", log.getResult().name());
        root.put("llmCallCount", capturingLlmClient.callCount());
        root.put("llmDelegateStatus", capturingLlmClient.delegateStatus());
        if (capturingLlmClient.delegateFailureType() != null) {
            root.put("llmDelegateFailureType", capturingLlmClient.delegateFailureType());
        }
        if (capturingLlmClient.delegateFailureCode() != null) {
            root.put("llmDelegateFailureCode", capturingLlmClient.delegateFailureCode());
        }
        root.put("indexStorageUri", promptSnapshot.indexStorageUri());
        root.put("selectedReferenceCount", promptSnapshot.selectedCount());
        root.put("sourceTextStoredInChecklistJson", false);

        ArrayNode hashes = root.putArray("selectedReferenceHashes");
        promptSnapshot.selectedHashes().forEach(hashes::add);
        ArrayNode ranges = root.putArray("selectedReferenceRangeLabels");
        promptSnapshot.selectedRangeLabels().forEach(ranges::add);

        Path output = projectDir()
                .resolve("build")
                .resolve("ai-review-reference")
                .resolve("layer2-real-index-sample-summary.json");
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), root);
    }

    private static Path realIndexPath() {
        return projectDir()
                .resolve("restricted")
                .resolve("validation")
                .resolve("index")
                .resolve("reference-index.json")
                .toAbsolutePath()
                .normalize();
    }

    static boolean realSampleEnabled() {
        return Boolean.getBoolean("qtai.ai.review.realSample")
                || "true".equalsIgnoreCase(System.getenv("QTAI_AI_REVIEW_REAL_SAMPLE"));
    }

    private static Path projectDir() {
        Path currentDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        if (Files.exists(currentDir.resolve("build.gradle.kts"))
                && Files.exists(currentDir.resolve("src").resolve("main"))) {
            return currentDir;
        }
        Path nestedProjectDir = currentDir.resolve("qtai-server");
        if (Files.exists(nestedProjectDir.resolve("build.gradle.kts"))
                && Files.exists(nestedProjectDir.resolve("src").resolve("main"))) {
            return nestedProjectDir;
        }
        return currentDir;
    }

    private static OffsetDateTime now() {
        return OffsetDateTime.now(KST_ZONE);
    }

    private record PromptReferenceSnapshot(
            String sourceFileHash,
            String indexStorageUri,
            int selectedCount,
            List<String> selectedHashes,
            List<String> selectedRangeLabels,
            List<String> selectedTexts
    ) {
    }

    @TestConfiguration
    static class LlmClientCaptureConfiguration {

        @Bean
        @Primary
        CapturingLlmClient capturingLlmClient(DeepSeekLlmClient delegate) {
            return new CapturingLlmClient(delegate);
        }
    }

    static class CapturingLlmClient implements LlmClient {

        private final DeepSeekLlmClient delegate;
        private int callCount;
        private LlmCompletionRequest lastRequest;
        private String delegateStatus = "NOT_CALLED";
        private String delegateFailureType;
        private String delegateFailureCode;

        CapturingLlmClient(DeepSeekLlmClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public LlmCompletionResponse complete(LlmCompletionRequest request) {
            callCount++;
            lastRequest = request;
            try {
                LlmCompletionResponse response = delegate.complete(request);
                delegateStatus = "COMPLETED";
                return response;
            } catch (RuntimeException exception) {
                delegateStatus = "THREW";
                delegateFailureType = exception.getClass().getSimpleName();
                if (exception instanceof BusinessException businessException) {
                    delegateFailureCode = businessException.getMessage();
                }
                throw exception;
            }
        }

        int callCount() {
            return callCount;
        }

        LlmCompletionRequest lastRequest() {
            return lastRequest;
        }

        String delegateStatus() {
            return delegateStatus;
        }

        String delegateFailureType() {
            return delegateFailureType;
        }

        String delegateFailureCode() {
            return delegateFailureCode;
        }
    }
}
