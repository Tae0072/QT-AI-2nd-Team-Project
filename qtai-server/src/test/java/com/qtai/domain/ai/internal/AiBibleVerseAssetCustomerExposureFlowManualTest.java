package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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

    // 사용자 응답(ExplanationItem)에서 aiAssetId가 제거되어(P2), 자산 연계 검증은 내부 read 모델로 한다.
    @Autowired
    private com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase listApprovedVerseExplanationUseCase;

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

        Map<Long, QtStudyContentResponse.ExplanationItem> visibleBeforeSeed =
                visibleExposuresByVerse(todayQt.qtPassageId());
        List<Long> jobIdsBeforeSeed = todayVerseJobIds(context.verseIds());
        AiDailyQtVerseExplanationSeedResult seedResult = seedService.seedToday();
        Map<Long, List<JobPick>> queuedJobsByVerse = queuedJobsByVerse(context.verseIds(), jobIdsBeforeSeed);
        List<VerseRunResult> verseResults = new ArrayList<>();

        for (Long verseId : context.verseIds()) {
            QtStudyContentResponse.ExplanationItem existingExposure = visibleBeforeSeed.get(verseId);
            if (existingExposure != null) {
                verseResults.add(VerseRunResult.alreadyVisible(verseId, existingExposure));
                continue;
            }

            JobPick jobPick = firstQueuedJob(queuedJobsByVerse.getOrDefault(verseId, List.of()));
            if (jobPick == null) {
                verseResults.add(VerseRunResult.noQueuedJob(verseId));
                continue;
            }

            VerseRunResult result = runAndExposeVerse(todayQt.qtPassageId(), verseId, jobPick);
            verseResults.add(result);
        }

        Map<Long, QtStudyContentResponse.ExplanationItem> finalExposureByVerse =
                visibleExposuresByVerse(todayQt.qtPassageId());
        String flowStatus = finalExposureByVerse.keySet().containsAll(context.verseIds())
                ? "FULLY_VISIBLE"
                : "PARTIAL_VISIBLE";
        writeFullSummary(
                flowStatus,
                todayQt,
                context,
                promptVersionId,
                checklistVersionId,
                referenceJobId,
                seedResult,
                verseResults,
                finalExposureByVerse
        );

        assertThat(finalExposureByVerse.keySet()).containsAll(context.verseIds());
        assertThat(finalExposureByVerse).hasSize(15);
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

    private Map<Long, List<JobPick>> queuedJobsByVerse(List<Long> verseIds, List<Long> jobIdsBeforeSeed) {
        String inClause = placeholders(verseIds.size());
        Map<Long, List<JobPick>> jobsByVerse = new LinkedHashMap<>();
        jdbcTemplate.query("""
                select target_id, id
                from ai_generation_jobs
                where job_type = 'EXPLANATION'
                  and target_type = 'BIBLE_VERSE'
                  and status = 'QUEUED'
                  and target_id in (%s)
                order by target_id asc, id asc
                """.formatted(inClause), rs -> {
                    Long verseId = rs.getLong(1);
                    Long jobId = rs.getLong(2);
                    String origin = jobIdsBeforeSeed.contains(jobId)
                            ? "PREEXISTING_QUEUED"
                            : "NEWLY_SEEDED";
                    jobsByVerse.computeIfAbsent(verseId, ignored -> new ArrayList<>())
                            .add(new JobPick(jobId, origin));
                }, verseIds.toArray());
        return jobsByVerse;
    }

    private static JobPick firstQueuedJob(List<JobPick> jobPicks) {
        return jobPicks.stream()
                .filter(jobPick -> "NEWLY_SEEDED".equals(jobPick.origin()))
                .findFirst()
                .or(() -> jobPicks.stream().findFirst())
                .orElse(null);
    }

    private VerseRunResult runAndExposeVerse(Long qtPassageId, Long verseId, JobPick jobPick) {
        boolean claimed = jobRunner.runJob(jobPick.id());
        AiGenerationJob job = generationJobRepository.findById(jobPick.id()).orElseThrow();
        if (!claimed) {
            return VerseRunResult.notClaimed(verseId, jobPick.origin(), job);
        }

        AiGeneratedAsset asset = assetByGenerationJobId(jobPick.id()).orElse(null);
        AiValidationLog layer1Log = asset == null ? null : latestValidationLog(
                asset.getId(),
                1,
                AiValidationReviewerType.AUTO
        );
        AiValidationLog layer2Log = asset == null ? null : latestValidationLog(
                asset.getId(),
                2,
                AiValidationReviewerType.ADVISOR
        );

        if (asset == null || layer1Log == null || layer2Log == null
                || layer1Log.getResult() != AiValidationResult.PASSED
                || layer2Log.getResult() != AiValidationResult.PASSED) {
            assertNoExposureForAsset(asset);
            return VerseRunResult.notApproved(verseId, jobPick.origin(), job, asset, layer1Log, layer2Log);
        }

        reviewAiAssetUseCase.reviewAiAsset(new ReviewAiAssetCommand(
                1L,
                asset.getId(),
                "ADMIN",
                "REVIEWER",
                "APPROVE",
                "manual customer exposure full-flow approval",
                true,
                now()
        ));

        AiGeneratedAsset approvedAsset = generatedAssetRepository.findById(asset.getId()).orElseThrow();
        assertThat(approvedAsset.getStatus()).isEqualTo(AiGeneratedAssetStatus.APPROVED);

        QtStudyContentResponse.ExplanationItem exposure = visibleExposureByVerse(qtPassageId, verseId);
        if (exposure == null) {
            return VerseRunResult.approvedButNotVisible(
                    verseId,
                    jobPick.origin(),
                    job,
                    approvedAsset,
                    layer1Log,
                    layer2Log
            );
        }
        // 사용자 노출 item에는 aiAssetId가 없으므로(P2), 자산 연계는 내부 승인 read 모델로 검증한다.
        assertThat(listApprovedVerseExplanationUseCase.listApprovedByVerseIds(java.util.List.of(verseId)))
                .as("승인된 자산이 사용자 노출 해설의 출처여야 한다")
                .anyMatch(approved -> asset.getId().equals(approved.aiAssetId()));
        return VerseRunResult.approvedAndVisible(
                verseId,
                jobPick.origin(),
                job,
                approvedAsset,
                layer1Log,
                layer2Log,
                exposure
        );
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

    private Map<Long, QtStudyContentResponse.ExplanationItem> visibleExposuresByVerse(Long qtPassageId) {
        Map<Long, QtStudyContentResponse.ExplanationItem> exposuresByVerse = new LinkedHashMap<>();
        for (QtStudyContentResponse.ExplanationItem item
                : getQtStudyContentUseCase.getStudyContent(qtPassageId).explanations()) {
            exposuresByVerse.putIfAbsent(item.verseId(), item);
        }
        return exposuresByVerse;
    }

    private QtStudyContentResponse.ExplanationItem visibleExposureByVerse(Long qtPassageId, Long verseId) {
        return visibleExposuresByVerse(qtPassageId).get(verseId);
    }

    private void writeFullSummary(
            String flowStatus,
            TodayQtResponse todayQt,
            QtPassageContentContext context,
            Long promptVersionId,
            Long checklistVersionId,
            Long referenceJobId,
            AiDailyQtVerseExplanationSeedResult seedResult,
            List<VerseRunResult> verseResults,
            Map<Long, QtStudyContentResponse.ExplanationItem> finalExposureByVerse
    ) throws IOException {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("schemaVersion", "ai-bible-verse-customer-exposure-full-summary.v1");
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
        root.put("finalVisibleCount", finalExposureByVerse.size());
        root.put("resultCount", verseResults.size());
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
        ArrayNode results = root.putArray("verseResults");
        for (VerseRunResult result : verseResults) {
            putVerseResult(results, result);
        }
        ArrayNode exposures = root.putArray("customerExposures");
        for (Long verseId : context.verseIds()) {
            QtStudyContentResponse.ExplanationItem exposure = finalExposureByVerse.get(verseId);
            if (exposure != null) {
                putExposure(exposures.addObject(), exposure);
            }
        }

        Path output = projectDir()
                .resolve("build")
                .resolve("ai-review-reference")
                .resolve("bible-verse-customer-exposure-full-summary.json");
        Files.createDirectories(output.getParent());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), root);
    }

    private void putVerseResult(ArrayNode results, VerseRunResult result) throws IOException {
        ObjectNode node = results.addObject();
        node.put("verseId", result.verseId());
        node.put("status", result.status());
        if (result.jobOrigin() != null) {
            node.put("jobOrigin", result.jobOrigin());
        }
        putJob(node, result.job());
        putAsset(node, "asset", result.asset());
        putValidationLog(node, "layer1", result.layer1Log());
        putValidationLog(node, "layer2", result.layer2Log());
        if (result.layer2Log() != null && result.layer2Log().getChecklistJson() != null) {
            JsonNode checklist = objectMapper.readTree(result.layer2Log().getChecklistJson());
            node.put("selectedReferenceExcerptCount", checklist.path("selectedReferenceExcerptCount").asInt(0));
            node.set("selectedReferenceHashes", checklist.path("selectedReferenceHashes"));
            node.set("selectedReferenceRangeLabels", checklist.path("selectedReferenceRangeLabels"));
        }
        if (result.exposure() != null) {
            putExposure(node.putObject("customerExposure"), result.exposure());
        }
    }

    private static void putExposure(ObjectNode node, QtStudyContentResponse.ExplanationItem exposure) {
        node.put("verseId", exposure.verseId());
        node.put("sourceLabel", exposure.sourceLabel());
        node.put("summary", exposure.summary());
        node.put("explanation", exposure.explanation());
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

    private record JobPick(
            Long id,
            String origin
    ) {
    }

    private record VerseRunResult(
            Long verseId,
            String status,
            String jobOrigin,
            AiGenerationJob job,
            AiGeneratedAsset asset,
            AiValidationLog layer1Log,
            AiValidationLog layer2Log,
            QtStudyContentResponse.ExplanationItem exposure
    ) {

        static VerseRunResult alreadyVisible(
                Long verseId,
                QtStudyContentResponse.ExplanationItem exposure
        ) {
            return new VerseRunResult(
                    verseId,
                    "ALREADY_VISIBLE",
                    null,
                    null,
                    null,
                    null,
                    null,
                    exposure
            );
        }

        static VerseRunResult noQueuedJob(Long verseId) {
            return new VerseRunResult(
                    verseId,
                    "NO_QUEUED_JOB",
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        static VerseRunResult notClaimed(
                Long verseId,
                String jobOrigin,
                AiGenerationJob job
        ) {
            return new VerseRunResult(
                    verseId,
                    "NOT_CLAIMED",
                    jobOrigin,
                    job,
                    null,
                    null,
                    null,
                    null
            );
        }

        static VerseRunResult notApproved(
                Long verseId,
                String jobOrigin,
                AiGenerationJob job,
                AiGeneratedAsset asset,
                AiValidationLog layer1Log,
                AiValidationLog layer2Log
        ) {
            return new VerseRunResult(
                    verseId,
                    "NOT_APPROVED",
                    jobOrigin,
                    job,
                    asset,
                    layer1Log,
                    layer2Log,
                    null
            );
        }

        static VerseRunResult approvedButNotVisible(
                Long verseId,
                String jobOrigin,
                AiGenerationJob job,
                AiGeneratedAsset approvedAsset,
                AiValidationLog layer1Log,
                AiValidationLog layer2Log
        ) {
            return new VerseRunResult(
                    verseId,
                    "APPROVED_BUT_NOT_VISIBLE",
                    jobOrigin,
                    job,
                    approvedAsset,
                    layer1Log,
                    layer2Log,
                    null
            );
        }

        static VerseRunResult approvedAndVisible(
                Long verseId,
                String jobOrigin,
                AiGenerationJob job,
                AiGeneratedAsset approvedAsset,
                AiValidationLog layer1Log,
                AiValidationLog layer2Log,
                QtStudyContentResponse.ExplanationItem exposure
        ) {
            return new VerseRunResult(
                    verseId,
                    "APPROVED_AND_VISIBLE",
                    jobOrigin,
                    job,
                    approvedAsset,
                    layer1Log,
                    layer2Log,
                    exposure
            );
        }
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
