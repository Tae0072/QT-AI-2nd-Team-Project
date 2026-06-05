package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClientException;

import com.qtai.common.exception.BusinessException;
import com.qtai.domain.ai.api.admin.asset.ReviewAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.ReviewAiAssetCommand;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.GetTodayQtUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qt.api.dto.TodayQtResponse;
import com.qtai.domain.study.api.GetQtStudyContentUseCase;
import com.qtai.domain.study.api.dto.QtStudyContentResponse;
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
        "ai.daily-qt-verse-seed.enabled=false",
        "external.llm.deepseek.api-key=${DEEPSEEK_API_KEY:}",
        "external.llm.deepseek.base-url=${DEEPSEEK_BASE_URL:https://api.deepseek.com}",
        "external.llm.deepseek.model=${DEEPSEEK_MODEL:deepseek-v4-flash}",
        "external.llm.deepseek.connect-timeout-ms=${DEEPSEEK_CONNECT_TIMEOUT_MS:3000}",
        "external.llm.deepseek.read-timeout-ms=${DEEPSEEK_READ_TIMEOUT_MS:30000}"
})
@ActiveProfiles("dev")
@EnabledIf("customerExposureSampleEnabled")
class AiBibleVerseAssetCustomerExposureFlowManualTest {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");
    private static final String SAMPLE_MARKER = "manual-customer-exposure-20260605";
    private static final String SAMPLE_VERSION = "manual-cust-exp-20260605";
    private static final Long TODAY_QT_PASSAGE_ID = 4L;
    private static final String TODAY_BOOK_CODE = "1CO";
    private static final int TODAY_CHAPTER = 3;
    private static final int TODAY_VERSE_FROM = 1;
    private static final int TODAY_VERSE_TO = 15;
    private static final String INDEX_STORAGE_URI = "restricted://validation/index/reference-index.json";
    private static final String SOURCE_FILE_HASH =
            "sha256:d50811d18c1d109a1ce0dc8331f25bb7daf249be1892d9ca742cbb64c20eca8b";

    @Autowired
    private GetTodayQtUseCase getTodayQtUseCase;

    @Autowired
    private GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;

    @Autowired
    private AiDailyQtVerseExplanationSeedService seedService;

    @Autowired
    private AiGenerationJobRunner jobRunner;

    @Autowired
    private AiGenerationJobRepository generationJobRepository;

    @Autowired
    private AiGeneratedAssetRepository generatedAssetRepository;

    @Autowired
    private AiValidationLogRepository validationLogRepository;

    @Autowired
    private ReviewAiAssetUseCase reviewAiAssetUseCase;

    @Autowired
    private GetQtStudyContentUseCase getQtStudyContentUseCase;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    void todayBibleVerseAssetGenerationValidationApprovalAndCustomerExposureFlow() throws Exception {
        assertThat(deepSeekApiKey).isNotBlank();
        assertThat(realIndexPath()).isRegularFile();

        ensureTodayQtVerseLinks();
        Long promptVersionId = ensureActiveExplanationPromptVersion();
        Long checklistVersionId = ensureActiveExplanationChecklistVersion();
        Long referenceJobId = ensureLatestActiveReferenceJob();

        TodayQtResponse todayQt = getTodayQtUseCase.getToday(null);
        assertThat(todayQt.qtPassageId()).isEqualTo(TODAY_QT_PASSAGE_ID);
        assertThat(todayQt.range().bookCode()).isEqualTo(TODAY_BOOK_CODE);
        assertThat(todayQt.range().chapter()).isEqualTo(TODAY_CHAPTER);
        assertThat(todayQt.range().verseFrom()).isEqualTo(TODAY_VERSE_FROM);
        assertThat(todayQt.range().verseTo()).isEqualTo(TODAY_VERSE_TO);

        QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(todayQt.qtPassageId());
        assertThat(context.verseIds()).hasSize(15);

        List<Long> jobIdsBeforeSeed = todayVerseJobIds(context.verseIds());
        AiDailyQtVerseExplanationSeedResult seedResult = seedService.seedToday();
        Long jobId = newlySeededJobIdToRun(context.verseIds(), jobIdsBeforeSeed);
        if (jobId == null) {
            QtStudyContentResponse.ExplanationItem existingExposure = firstVisibleExposure(todayQt.qtPassageId());
            writeSummary(
                    "NO_QUEUED_JOB",
                    todayQt,
                    context,
                    promptVersionId,
                    checklistVersionId,
                    referenceJobId,
                    seedResult,
                    null,
                    null,
                    null,
                    null,
                    null,
                    existingExposure
            );
            assertThat(existingExposure)
                    .as("seedToday created no job, so an existing customer exposure row must already be visible")
                    .isNotNull();
            return;
        }

        boolean claimed = jobRunner.runJob(jobId);
        assertThat(claimed).isTrue();

        AiGenerationJob job = generationJobRepository.findById(jobId).orElseThrow();
        AiGeneratedAsset asset = assetByGenerationJobId(jobId).orElse(null);
        AiValidationLog layer1Log = asset == null ? null : latestValidationLog(asset.getId(), 1, AiValidationReviewerType.AUTO);
        AiValidationLog layer2Log = asset == null ? null : latestValidationLog(asset.getId(), 2, AiValidationReviewerType.ADVISOR);

        if (asset == null || layer1Log == null || layer2Log == null
                || layer1Log.getResult() != AiValidationResult.PASSED
                || layer2Log.getResult() != AiValidationResult.PASSED) {
            writeSummary(
                    "NOT_APPROVED",
                    todayQt,
                    context,
                    promptVersionId,
                    checklistVersionId,
                    referenceJobId,
                    seedResult,
                    job,
                    asset,
                    layer1Log,
                    layer2Log,
                    null,
                    null
            );
            assertNoExposureForAsset(asset);
            return;
        }

        reviewAiAssetUseCase.reviewAiAsset(new ReviewAiAssetCommand(
                1L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "APPROVE",
                "manual customer exposure sample approval",
                true,
                now()
        ));

        AiGeneratedAsset approvedAsset = generatedAssetRepository.findById(asset.getId()).orElseThrow();
        assertThat(approvedAsset.getStatus()).isEqualTo(AiGeneratedAssetStatus.APPROVED);

        QtStudyContentResponse.ExplanationItem exposure = visibleExposure(todayQt.qtPassageId(), asset.getId());
        assertThat(exposure).isNotNull();
        assertThat(exposure.verseId()).isEqualTo(asset.getTargetId());
        assertThat(exposure.aiAssetId()).isEqualTo(asset.getId());

        writeSummary(
                "APPROVED_AND_VISIBLE",
                todayQt,
                context,
                promptVersionId,
                checklistVersionId,
                referenceJobId,
                seedResult,
                job,
                approvedAsset,
                layer1Log,
                layer2Log,
                approvedAsset,
                exposure
        );
    }

    private void ensureTodayQtVerseLinks() {
        assertThat(count("""
                select count(*)
                from bible_verses
                where book_id = 46
                  and chapter_no = 3
                  and verse_no between 1 and 15
                """)).isEqualTo(15);
        jdbcTemplate.update("""
                insert into qt_passage_verses (qt_passage_id, bible_verse_id, display_order)
                select ?, bv.id, bv.verse_no
                from bible_verses bv
                where bv.book_id = 46
                  and bv.chapter_no = 3
                  and bv.verse_no between 1 and 15
                  and not exists (
                      select 1
                      from qt_passage_verses qpv
                      where qpv.qt_passage_id = ?
                        and qpv.bible_verse_id = bv.id
                  )
                order by bv.verse_no
                """, TODAY_QT_PASSAGE_ID, TODAY_QT_PASSAGE_ID);
        assertThat(count("""
                select count(*)
                from qt_passage_verses
                where qt_passage_id = ?
                """, TODAY_QT_PASSAGE_ID)).isEqualTo(15);
    }

    private Long ensureActiveExplanationPromptVersion() {
        if (count("""
                select count(*)
                from ai_prompt_versions
                where prompt_type = 'EXPLANATION'
                  and status = 'ACTIVE'
                """) == 0) {
            Long existingId = optionalLong("""
                    select id
                    from ai_prompt_versions
                    where prompt_type = 'EXPLANATION'
                      and version = ?
                    order by id desc
                    limit 1
                    """, SAMPLE_VERSION);
            if (existingId == null) {
                jdbcTemplate.update("""
                        insert into ai_prompt_versions
                            (prompt_type, version, content_hash, status, created_at)
                        values ('EXPLANATION', ?, ?, 'ACTIVE', current_timestamp)
                        """, SAMPLE_VERSION, "sha256:" + SAMPLE_VERSION + "-prompt");
            } else {
                jdbcTemplate.update("""
                        update ai_prompt_versions
                        set status = 'ACTIVE',
                            created_at = current_timestamp
                        where id = ?
                        """, existingId);
            }
        }
        Long promptVersionId = optionalLong("""
                select id
                from ai_prompt_versions
                where prompt_type = 'EXPLANATION'
                  and status = 'ACTIVE'
                order by created_at desc, id desc
                limit 1
                """);
        assertThat(promptVersionId).isNotNull();
        return promptVersionId;
    }

    private Long ensureActiveExplanationChecklistVersion() {
        int activeCount = count("""
                select count(*)
                from ai_validation_checklist_versions
                where checklist_type = 'EXPLANATION'
                  and status = 'ACTIVE'
                """);
        assertThat(activeCount)
                .as("AiAutoValidationService and AiReviewValidationService require one active EXPLANATION checklist")
                .isLessThanOrEqualTo(1);
        if (activeCount == 0) {
            Long existingId = optionalLong("""
                    select id
                    from ai_validation_checklist_versions
                    where checklist_type = 'EXPLANATION'
                      and version = ?
                    order by id desc
                    limit 1
                    """, SAMPLE_VERSION);
            if (existingId == null) {
                jdbcTemplate.update("""
                        insert into ai_validation_checklist_versions
                            (checklist_type, version, content_hash, status, created_by_admin_id, created_at, activated_at)
                        values ('EXPLANATION', ?, ?, 'ACTIVE', null, current_timestamp, current_timestamp)
                        """, SAMPLE_VERSION, "sha256:" + SAMPLE_VERSION + "-checklist");
            } else {
                jdbcTemplate.update("""
                        update ai_validation_checklist_versions
                        set status = 'ACTIVE',
                            activated_at = current_timestamp,
                            retired_at = null
                        where id = ?
                        """, existingId);
            }
        }
        Long checklistVersionId = optionalLong("""
                select id
                from ai_validation_checklist_versions
                where checklist_type = 'EXPLANATION'
                  and status = 'ACTIVE'
                order by id desc
                limit 1
                """);
        assertThat(checklistVersionId).isNotNull();
        return checklistVersionId;
    }

    private Long ensureLatestActiveReferenceJob() {
        Long existingId = optionalLong("""
                select id
                from validation_reference_jobs
                where source_name = ?
                  and index_storage_uri = ?
                order by id desc
                limit 1
                """, SAMPLE_MARKER, INDEX_STORAGE_URI);
        if (existingId == null) {
            jdbcTemplate.update("""
                    insert into validation_reference_jobs
                        (source_name, source_file_name, source_file_hash, storage_uri, index_storage_uri,
                         status, expires_at, created_at, updated_at)
                    values (?, 'manual-reference.pdf', ?, 'restricted://validation/reference.pdf', ?,
                            'ACTIVE', date_add(current_timestamp, interval 30 day), current_timestamp, current_timestamp)
                    """, SAMPLE_MARKER, SOURCE_FILE_HASH, INDEX_STORAGE_URI);
        } else {
            jdbcTemplate.update("""
                    update validation_reference_jobs
                    set status = 'ACTIVE',
                        source_file_hash = ?,
                        index_storage_uri = ?,
                        deleted_at = null,
                        expires_at = date_add(current_timestamp, interval 30 day),
                        created_at = current_timestamp,
                        updated_at = current_timestamp
                    where id = ?
                    """, SOURCE_FILE_HASH, INDEX_STORAGE_URI, existingId);
        }
        Long referenceJobId = optionalLong("""
                select id
                from validation_reference_jobs
                where status = 'ACTIVE'
                  and index_storage_uri = ?
                order by created_at desc, id desc
                limit 1
                """, INDEX_STORAGE_URI);
        assertThat(referenceJobId).isNotNull();
        return referenceJobId;
    }

    private List<Long> todayVerseJobIds(List<Long> verseIds) {
        String inClause = placeholders(verseIds.size());
        return jdbcTemplate.query("""
                select id
                from ai_generation_jobs
                where job_type = 'EXPLANATION'
                  and target_type = 'BIBLE_VERSE'
                  and target_id in (%s)
                order by id asc
                """.formatted(inClause), (rs, rowNum) -> rs.getLong(1), verseIds.toArray());
    }

    private Long newlySeededJobIdToRun(List<Long> verseIds, List<Long> jobIdsBeforeSeed) {
        String inClause = placeholders(verseIds.size());
        List<Long> queuedJobIds = jdbcTemplate.query("""
                select id
                from ai_generation_jobs
                where job_type = 'EXPLANATION'
                  and target_type = 'BIBLE_VERSE'
                  and status = 'QUEUED'
                  and target_id in (%s)
                order by id asc
                """.formatted(inClause), (rs, rowNum) -> rs.getLong(1), verseIds.toArray());
        // Keep the sample run tied to this seed execution; stale QUEUED jobs make provenance unclear.
        return queuedJobIds.stream()
                .filter(id -> !jobIdsBeforeSeed.contains(id))
                .findFirst()
                .orElse(null);
    }

    private Optional<AiGeneratedAsset> assetByGenerationJobId(Long generationJobId) {
        Long assetId = optionalLong("""
                select id
                from ai_generated_assets
                where generation_job_id = ?
                order by id desc
                limit 1
                """, generationJobId);
        if (assetId == null) {
            return Optional.empty();
        }
        return generatedAssetRepository.findById(assetId);
    }

    private AiValidationLog latestValidationLog(
            Long assetId,
            int layer,
            AiValidationReviewerType reviewerType
    ) {
        return validationLogRepository.findFirstByAiAssetIdAndLayerAndReviewerTypeOrderByCreatedAtDescIdDesc(
                assetId,
                layer,
                reviewerType
        ).orElse(null);
    }

    private void assertNoExposureForAsset(AiGeneratedAsset asset) {
        if (asset == null || asset.getId() == null) {
            return;
        }
        Integer exposureCount = count("""
                select count(*)
                from verse_explanations
                where ai_asset_id = ?
                  and status = 'APPROVED'
                  and active_unique_key = 'ACTIVE'
                """, asset.getId());
        assertThat(exposureCount).isZero();
    }

    private QtStudyContentResponse.ExplanationItem visibleExposure(Long qtPassageId, Long assetId) {
        return getQtStudyContentUseCase.getStudyContent(qtPassageId)
                .explanations()
                .stream()
                .filter(item -> assetId.equals(item.aiAssetId()))
                .findFirst()
                .orElse(null);
    }

    private QtStudyContentResponse.ExplanationItem firstVisibleExposure(Long qtPassageId) {
        List<QtStudyContentResponse.ExplanationItem> explanations =
                getQtStudyContentUseCase.getStudyContent(qtPassageId).explanations();
        if (explanations.isEmpty()) {
            return null;
        }
        return explanations.get(0);
    }

    private void writeSummary(
            String flowStatus,
            TodayQtResponse todayQt,
            QtPassageContentContext context,
            Long promptVersionId,
            Long checklistVersionId,
            Long referenceJobId,
            AiDailyQtVerseExplanationSeedResult seedResult,
            AiGenerationJob job,
            AiGeneratedAsset asset,
            AiValidationLog layer1Log,
            AiValidationLog layer2Log,
            AiGeneratedAsset approvedAsset,
            QtStudyContentResponse.ExplanationItem exposure
    ) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", "ai-bible-verse-customer-exposure-summary.v1");
        root.put("flowStatus", flowStatus);
        root.put("sampleMarker", SAMPLE_MARKER);
        root.put("sampleVersion", SAMPLE_VERSION);
        root.put("qtPassageId", todayQt.qtPassageId());
        root.put("rangeBookCode", todayQt.range().bookCode());
        root.put("rangeChapter", todayQt.range().chapter());
        root.put("rangeVerseFrom", todayQt.range().verseFrom());
        root.put("rangeVerseTo", todayQt.range().verseTo());
        root.put("promptVersionId", promptVersionId);
        root.put("checklistVersionId", checklistVersionId);
        root.put("validationReferenceJobId", referenceJobId);
        root.put("indexStorageUri", INDEX_STORAGE_URI);
        root.put("sourceFileHash", SOURCE_FILE_HASH);
        root.put("seedCreatedCount", seedResult.createdCount());
        root.put("seedFailedCount", seedResult.failedCount());
        if (seedResult.failureReason() != null) {
            root.put("seedFailureReason", seedResult.failureReason());
        }
        ArrayNode verseIds = root.putArray("verseIds");
        context.verseIds().forEach(verseIds::add);
        putJob(root, job);
        putAsset(root, "asset", asset);
        putValidationLog(root, "layer1", layer1Log);
        putValidationLog(root, "layer2", layer2Log);
        putAsset(root, "approvedAsset", approvedAsset);
        root.put("llmCallCount", capturingLlmClient.callCount());
        ArrayNode llmCalls = root.putArray("llmCalls");
        capturingLlmClient.callRecords().forEach(record -> {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("status", record.status());
            if (record.failureType() != null) {
                node.put("failureType", record.failureType());
            }
            if (record.failureCode() != null) {
                node.put("failureCode", record.failureCode());
            }
            llmCalls.add(node);
        });
        if (layer2Log != null && layer2Log.getChecklistJson() != null) {
            JsonNode checklist = objectMapper.readTree(layer2Log.getChecklistJson());
            root.put("selectedReferenceExcerptCount", checklist.path("selectedReferenceExcerptCount").asInt(0));
            root.set("selectedReferenceHashes", checklist.path("selectedReferenceHashes"));
            root.set("selectedReferenceRangeLabels", checklist.path("selectedReferenceRangeLabels"));
        }
        if (exposure != null) {
            ObjectNode exposureNode = root.putObject("customerExposure");
            exposureNode.put("verseId", exposure.verseId());
            exposureNode.put("aiAssetId", exposure.aiAssetId());
            exposureNode.put("sourceLabel", exposure.sourceLabel());
            exposureNode.put("summary", exposure.summary());
            exposureNode.put("explanation", exposure.explanation());
        }

        Path output = projectDir()
                .resolve("build")
                .resolve("ai-review-reference")
                .resolve("bible-verse-customer-exposure-summary.json");
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), root);
    }

    private void putJob(ObjectNode root, AiGenerationJob job) {
        if (job == null) {
            return;
        }
        ObjectNode jobNode = root.putObject("generationJob");
        jobNode.put("id", job.getId());
        jobNode.put("status", job.getStatus().name());
        jobNode.put("targetType", job.getTargetType().name());
        jobNode.put("targetId", job.getTargetId());
        if (job.getErrorMessage() != null) {
            jobNode.put("errorMessage", job.getErrorMessage());
        }
    }

    private void putAsset(ObjectNode root, String fieldName, AiGeneratedAsset asset) {
        if (asset == null) {
            return;
        }
        ObjectNode assetNode = root.putObject(fieldName);
        assetNode.put("id", asset.getId());
        assetNode.put("status", asset.getStatus().name());
        assetNode.put("assetType", asset.getAssetType().name());
        assetNode.put("targetType", asset.getTargetType().name());
        assetNode.put("targetId", asset.getTargetId());
    }

    private void putValidationLog(ObjectNode root, String fieldName, AiValidationLog log) {
        if (log == null) {
            return;
        }
        ObjectNode logNode = root.putObject(fieldName);
        logNode.put("id", log.getId());
        logNode.put("layer", log.getLayer());
        logNode.put("reviewerType", log.getReviewerType().name());
        logNode.put("result", log.getResult().name());
        if (log.getValidationReferenceJobId() != null) {
            logNode.put("validationReferenceJobId", log.getValidationReferenceJobId());
        }
        if (log.getErrorMessage() != null) {
            logNode.put("errorMessage", log.getErrorMessage());
        }
    }

    private Integer count(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        assertThat(count).isNotNull();
        return count;
    }

    private Long optionalLong(String sql, Object... args) {
        List<Long> ids = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getLong(1), args);
        if (ids.isEmpty()) {
            return null;
        }
        return ids.get(0);
    }

    private static String placeholders(int size) {
        return String.join(",", java.util.Collections.nCopies(size, "?"));
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

    static boolean customerExposureSampleEnabled() {
        return Boolean.getBoolean("qtai.ai.customerExposureSample")
                || "true".equalsIgnoreCase(System.getenv("QTAI_AI_CUSTOMER_EXPOSURE_SAMPLE"));
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
        private final List<CallRecord> callRecords = new ArrayList<>();

        CapturingLlmClient(DeepSeekLlmClient delegate) {
            this.delegate = delegate;
        }

        @Override
        public LlmCompletionResponse complete(LlmCompletionRequest request) {
            try {
                LlmCompletionResponse response = delegate.complete(request);
                callRecords.add(new CallRecord("COMPLETED", null, null));
                return response;
            } catch (BusinessException exception) {
                callRecords.add(new CallRecord(
                        "THREW",
                        exception.getClass().getSimpleName(),
                        exception.getMessage()
                ));
                throw exception;
            } catch (RestClientException exception) {
                callRecords.add(new CallRecord(
                        "THREW",
                        exception.getClass().getSimpleName(),
                        null
                ));
                throw exception;
            }
        }

        int callCount() {
            return callRecords.size();
        }

        List<CallRecord> callRecords() {
            return List.copyOf(callRecords);
        }
    }

    private record CallRecord(
            String status,
            String failureType,
            String failureCode
    ) {
    }
}
