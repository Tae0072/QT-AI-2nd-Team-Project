package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import com.qtai.config.JpaAuditingConfig;

@DataJpaTest
@ActiveProfiles("test")
@Import({AdminAiMonitoringQueryRepository.class, JpaAuditingConfig.class})
class AdminAiMonitoringQueryRepositoryTest {

    private static final OffsetDateTime PERIOD_FROM = OffsetDateTime.parse("2026-06-02T00:00:00+09:00");
    private static final OffsetDateTime PERIOD_TO_EXCLUSIVE = OffsetDateTime.parse("2026-06-03T00:00:00+09:00");
    private static final LocalDateTime CREATED_FROM = LocalDateTime.parse("2026-06-02T00:00:00");
    private static final LocalDateTime CREATED_TO_EXCLUSIVE = LocalDateTime.parse("2026-06-03T00:00:00");

    @Autowired
    private TestEntityManager testEntityManager;

    @Autowired
    private AiBatchRunLogRepository batchRunLogRepository;

    @Autowired
    private AdminAiMonitoringQueryRepository repository;

    @Test
    void summarizeAggregatesOperationalMonitoringCounts() {
        AiPromptVersion promptVersion = persistPromptVersion();
        persistJob(promptVersion, AiGenerationJobStatus.QUEUED, PERIOD_FROM.minusDays(3), null);
        persistJob(promptVersion, AiGenerationJobStatus.RUNNING, PERIOD_FROM.minusDays(2), null);
        persistJob(promptVersion, AiGenerationJobStatus.SUCCEEDED, PERIOD_FROM.minusHours(2), PERIOD_FROM.plusHours(1));
        persistJob(promptVersion, AiGenerationJobStatus.FAILED, PERIOD_FROM.minusHours(1), PERIOD_FROM.plusHours(2));
        persistJob(promptVersion, AiGenerationJobStatus.SUCCEEDED, PERIOD_FROM.minusHours(1), PERIOD_TO_EXCLUSIVE.plusHours(1));

        persistAsset(AiGeneratedAssetStatus.VALIDATING);
        persistAsset(AiGeneratedAssetStatus.APPROVED);

        AiValidationChecklistVersion explanationChecklist = persistActiveChecklist(
                AiValidationChecklistType.EXPLANATION,
                "2026.06.1"
        );
        AiValidationChecklistVersion qaChecklist = persistActiveChecklist(AiValidationChecklistType.QA, "2026.06.1");

        persistValidationLog(AiValidationResult.PASSED, null, explanationChecklist.getId(), PERIOD_FROM.plusMinutes(1));
        persistValidationLog(AiValidationResult.REJECTED, "SOURCE_MISSING", explanationChecklist.getId(),
                PERIOD_FROM.plusMinutes(2));
        persistValidationLog(AiValidationResult.REJECTED, "SOURCE_MISSING", null, PERIOD_FROM.plusMinutes(3));
        persistValidationLog(AiValidationResult.REJECTED, " ", null, PERIOD_FROM.plusMinutes(4));
        persistValidationLog(AiValidationResult.NEEDS_REVIEW, null, explanationChecklist.getId(),
                PERIOD_FROM.plusMinutes(5));
        persistValidationLog(AiValidationResult.REJECTED, "OUT_OF_PERIOD", null, PERIOD_TO_EXCLUSIVE.plusMinutes(1));

        AiBatchRunLog oldestFailure = persistBatchRunLog(
                AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED,
                AiBatchRunStatus.FAILED,
                "oldest failure",
                CREATED_FROM.plusMinutes(1)
        );
        persistBatchRunLog(AiBatchName.AI_GENERATION_WORKER_POLL, AiBatchRunStatus.SUCCEEDED, null,
                CREATED_FROM.plusMinutes(2));
        persistBatchRunLog(AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED, AiBatchRunStatus.PARTIAL_FAILED,
                "partial 1", CREATED_FROM.plusMinutes(3));
        persistBatchRunLog(AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED, AiBatchRunStatus.PARTIAL_FAILED,
                "partial 2", CREATED_FROM.plusMinutes(4));
        persistBatchRunLog(AiBatchName.AI_GENERATION_WORKER_POLL, AiBatchRunStatus.FAILED, "worker failed",
                CREATED_FROM.plusMinutes(5));
        persistBatchRunLog(AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED, AiBatchRunStatus.FAILED, "latest failed",
                CREATED_FROM.plusMinutes(6));
        persistBatchRunLog(AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED, AiBatchRunStatus.PARTIAL_FAILED,
                "latest partial", CREATED_FROM.plusMinutes(7));
        persistBatchRunLog(AiBatchName.AI_DAILY_QT_VERSE_EXPLANATION_SEED, AiBatchRunStatus.FAILED,
                "out of period failed", CREATED_TO_EXCLUSIVE.plusMinutes(1));
        flushAndClear();

        AdminAiMonitoringQueryRepository.Summary summary = repository.summarize(filter());

        assertThat(summary.generationJobs())
                .extracting(
                        AdminAiMonitoringQueryRepository.GenerationJobCounts::queued,
                        AdminAiMonitoringQueryRepository.GenerationJobCounts::running,
                        AdminAiMonitoringQueryRepository.GenerationJobCounts::succeeded,
                        AdminAiMonitoringQueryRepository.GenerationJobCounts::failed
                )
                .containsExactly(1L, 1L, 1L, 1L);
        assertThat(summary.validation())
                .extracting(
                        AdminAiMonitoringQueryRepository.ValidationCounts::waitingAssets,
                        AdminAiMonitoringQueryRepository.ValidationCounts::passCount,
                        AdminAiMonitoringQueryRepository.ValidationCounts::failCount,
                        AdminAiMonitoringQueryRepository.ValidationCounts::needsReviewCount
                )
                .containsExactly(1L, 1L, 3L, 1L);
        assertThat(summary.failureReasons())
                .extracting(
                        AdminAiMonitoringQueryRepository.FailureReasonRow::resultCode,
                        AdminAiMonitoringQueryRepository.FailureReasonRow::count
                )
                .containsExactly(tuple("SOURCE_MISSING", 2L), tuple("REJECTED", 1L));
        assertThat(summary.batchRuns())
                .extracting(
                        AdminAiMonitoringQueryRepository.BatchRunCounts::succeeded,
                        AdminAiMonitoringQueryRepository.BatchRunCounts::partialFailed,
                        AdminAiMonitoringQueryRepository.BatchRunCounts::failed
                )
                .containsExactly(1L, 3L, 3L);
        assertThat(summary.latestBatchFailures())
                .hasSize(5)
                .extracting(AdminAiMonitoringQueryRepository.BatchFailureRow::id)
                .doesNotContain(oldestFailure.getId());
        assertThat(summary.checklists())
                .extracting(
                        AdminAiMonitoringQueryRepository.ChecklistRow::checklistType,
                        AdminAiMonitoringQueryRepository.ChecklistRow::activeVersion,
                        AdminAiMonitoringQueryRepository.ChecklistRow::passCount,
                        AdminAiMonitoringQueryRepository.ChecklistRow::totalCount
                )
                .containsExactly(
                        tuple(AiValidationChecklistType.EXPLANATION, "2026.06.1", 1L, 3L),
                        tuple(AiValidationChecklistType.QA, "2026.06.1", 0L, 0L)
                );
        assertThat(summary.checklists().get(1).activeVersion()).isEqualTo(qaChecklist.getVersion());
    }

    @Test
    void summarizeLimitsFailureReasonsToTopTenByCountThenCode() {
        for (int index = 0; index < 11; index++) {
            persistValidationLog(AiValidationResult.REJECTED, "REASON_" + String.format("%02d", index), null,
                    PERIOD_FROM.plusMinutes(index));
        }
        persistValidationLog(AiValidationResult.REJECTED, "SOURCE_MISSING", null, PERIOD_FROM.plusHours(1));
        persistValidationLog(AiValidationResult.REJECTED, "SOURCE_MISSING", null, PERIOD_FROM.plusHours(2));
        flushAndClear();

        AdminAiMonitoringQueryRepository.Summary summary = repository.summarize(filter());

        assertThat(summary.failureReasons()).hasSize(10);
        assertThat(summary.failureReasons().get(0).resultCode()).isEqualTo("SOURCE_MISSING");
        assertThat(summary.failureReasons().get(0).count()).isEqualTo(2L);
    }

    private AdminAiMonitoringQueryRepository.Filter filter() {
        return new AdminAiMonitoringQueryRepository.Filter(
                PERIOD_FROM,
                PERIOD_TO_EXCLUSIVE,
                CREATED_FROM,
                CREATED_TO_EXCLUSIVE
        );
    }

    private AiPromptVersion persistPromptVersion() {
        AiPromptVersion promptVersion = new AiPromptVersion();
        setField(promptVersion, "promptType", AiPromptType.EXPLANATION);
        setField(promptVersion, "version", "2026.06.1");
        setField(promptVersion, "contentHash", "sha256:monitoring-prompt");
        setField(promptVersion, "status", AiPromptVersionStatus.ACTIVE);
        setField(promptVersion, "createdAt", PERIOD_FROM.minusDays(1));
        return testEntityManager.persistAndFlush(promptVersion);
    }

    private AiGenerationJob persistJob(
            AiPromptVersion promptVersion,
            AiGenerationJobStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime finishedAt
    ) {
        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                nextTargetId(),
                promptVersion.getId(),
                createdAt
        );
        if (status == AiGenerationJobStatus.RUNNING) {
            job.markRunning(createdAt.plusSeconds(1));
        } else if (status == AiGenerationJobStatus.SUCCEEDED) {
            job.markRunning(createdAt.plusSeconds(1));
            job.markSucceeded(finishedAt);
        } else if (status == AiGenerationJobStatus.FAILED) {
            job.markFailed("TEST_FAILURE", finishedAt);
        }
        return testEntityManager.persistAndFlush(job);
    }

    private void persistAsset(AiGeneratedAssetStatus status) {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                nextGenerationJobId(),
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                nextTargetId(),
                "{}",
                "test-source",
                PERIOD_FROM.plusMinutes(1)
        );
        if (status == AiGeneratedAssetStatus.APPROVED) {
            asset.approve(PERIOD_FROM.plusMinutes(2));
        } else if (status == AiGeneratedAssetStatus.REJECTED) {
            asset.reject(PERIOD_FROM.plusMinutes(2));
        } else if (status == AiGeneratedAssetStatus.HIDDEN) {
            asset.hide(PERIOD_FROM.plusMinutes(2));
        }
        testEntityManager.persistAndFlush(asset);
    }

    private AiValidationChecklistVersion persistActiveChecklist(AiValidationChecklistType type, String version) {
        AiValidationChecklistVersion checklistVersion = AiValidationChecklistVersion.create(
                type,
                version,
                "sha256:" + type + "-" + version,
                null,
                PERIOD_FROM.minusHours(1)
        );
        checklistVersion.activate(PERIOD_FROM.minusMinutes(30));
        return testEntityManager.persistAndFlush(checklistVersion);
    }

    private void persistValidationLog(
            AiValidationResult result,
            String errorMessage,
            Long checklistVersionId,
            OffsetDateTime createdAt
    ) {
        testEntityManager.persistAndFlush(AiValidationLog.create(
                nextAssetId(),
                null,
                1,
                result,
                AiValidationReviewerType.AUTO,
                checklistVersionId,
                null,
                errorMessage,
                createdAt
        ));
    }

    private AiBatchRunLog persistBatchRunLog(
            AiBatchName batchName,
            AiBatchRunStatus status,
            String errorMessage,
            LocalDateTime createdAt
    ) {
        OffsetDateTime startedAt = PERIOD_FROM.plusMinutes(5);
        AiBatchRunLog log = batchRunLogRepository.save(AiBatchRunLog.create(new AiBatchRunLogCommand(
                batchName,
                status,
                status == AiBatchRunStatus.SUCCEEDED ? 1 : 0,
                status == AiBatchRunStatus.FAILED || status == AiBatchRunStatus.PARTIAL_FAILED ? 1 : 0,
                batchName == AiBatchName.AI_GENERATION_WORKER_POLL ? 1 : 0,
                errorMessage == null ? null : "IllegalStateException",
                errorMessage,
                startedAt,
                startedAt.plusSeconds(1)
        )));
        batchRunLogRepository.flush();
        testEntityManager.getEntityManager()
                .createNativeQuery("update ai_batch_run_logs set created_at = ? where id = ?")
                .setParameter(1, Timestamp.valueOf(createdAt))
                .setParameter(2, log.getId())
                .executeUpdate();
        setField(log, "createdAt", createdAt);
        return log;
    }

    private void flushAndClear() {
        testEntityManager.flush();
        testEntityManager.clear();
    }

    private long nextTargetId = 100L;
    private long nextGenerationJobId = 1_000L;
    private long nextAssetId = 2_000L;

    private Long nextTargetId() {
        return nextTargetId++;
    }

    private Long nextGenerationJobId() {
        return nextGenerationJobId++;
    }

    private Long nextAssetId() {
        return nextAssetId++;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
