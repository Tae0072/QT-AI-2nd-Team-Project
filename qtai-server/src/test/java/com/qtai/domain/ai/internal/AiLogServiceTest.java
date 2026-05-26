package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Modifier;
import java.time.OffsetDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class AiLogServiceTest {

    private AiGenerationJobRepository generationJobRepository;
    private AiGeneratedAssetRepository generatedAssetRepository;
    private AiValidationLogRepository validationLogRepository;
    private AiLogService aiLogService;

    @BeforeEach
    void setUp() {
        generationJobRepository = org.mockito.Mockito.mock(AiGenerationJobRepository.class);
        generatedAssetRepository = org.mockito.Mockito.mock(AiGeneratedAssetRepository.class);
        validationLogRepository = org.mockito.Mockito.mock(AiValidationLogRepository.class);
        aiLogService = new AiLogService(generationJobRepository, generatedAssetRepository, validationLogRepository);
    }

    @Test
    void 생성작업은_QUEUED_상태와_대상정보를_기록한다() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-20T04:00:00+09:00");
        when(generationJobRepository.save(any(AiGenerationJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiGenerationJob job = aiLogService.queueGeneration(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                now
        );

        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.QUEUED);
        assertThat(job.getJobType()).isEqualTo(AiGenerationJobType.EXPLANATION);
        assertThat(job.getTargetType()).isEqualTo(AiTargetType.QT_PASSAGE);
        assertThat(job.getTargetId()).isEqualTo(35L);
        assertThat(job.getPromptVersionId()).isEqualTo(3L);
        assertThat(job.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void 생성실패는_FAILED_상태와_재처리_대상정보를_유지한다() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-20T04:00:00+09:00");
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-20T04:01:00+09:00");
        OffsetDateTime failedAt = OffsetDateTime.parse("2026-05-20T04:02:00+09:00");
        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.SIMULATOR,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                createdAt
        );
        job.markRunning(startedAt);
        when(generationJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(generationJobRepository.save(any(AiGenerationJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiGenerationJob failedJob = aiLogService.markGenerationFailed(1L, "LLM_TIMEOUT", failedAt);

        assertThat(failedJob.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(failedJob.getErrorMessage()).isEqualTo("LLM_TIMEOUT");
        assertThat(failedJob.getTargetType()).isEqualTo(AiTargetType.QT_PASSAGE);
        assertThat(failedJob.getTargetId()).isEqualTo(35L);
        assertThat(failedJob.getFinishedAt()).isEqualTo(failedAt);
    }

    @Test
    void 산출물등록은_VALIDATING_상태와_출처표기를_기록한다() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-20T04:03:00+09:00");
        when(generatedAssetRepository.save(any(AiGeneratedAsset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiGeneratedAsset asset = aiLogService.registerGeneratedAsset(
                1L,
                AiGeneratedAssetType.EXPLANATION,
                AiTargetType.BIBLE_VERSE,
                1001L,
                "{\"summary\":\"검증 대기 해설\"}",
                "QT-AI verified content",
                now
        );

        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        assertThat(asset.getGenerationJobId()).isEqualTo(1L);
        assertThat(asset.getAssetType()).isEqualTo(AiGeneratedAssetType.EXPLANATION);
        assertThat(asset.getSourceLabel()).isEqualTo("QT-AI verified content");
        assertThat(asset.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void 검증실패는_검증로그를_남기고_산출물을_APPROVED로_만들지_않는다() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-20T04:03:00+09:00");
        OffsetDateTime validatedAt = OffsetDateTime.parse("2026-05-20T04:04:00+09:00");
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.QA_RESPONSE,
                AiTargetType.QA_REQUEST,
                700L,
                "{\"answer\":\"검증 대기 답변\"}",
                "QT-AI verified content",
                createdAt
        );
        when(generatedAssetRepository.findById(2L)).thenReturn(Optional.of(asset));
        when(generatedAssetRepository.save(any(AiGeneratedAsset.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(validationLogRepository.save(any(AiValidationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiValidationLog log = aiLogService.registerValidationLog(
                2L,
                2,
                AiValidationResult.REJECTED,
                AiValidationReviewerType.AUTO,
                4L,
                "{\"result\":\"rejected\"}",
                "POLICY_VIOLATION",
                validatedAt
        );

        assertThat(log.getResult()).isEqualTo(AiValidationResult.REJECTED);
        assertThat(log.getReviewerType()).isEqualTo(AiValidationReviewerType.AUTO);
        assertThat(log.getChecklistVersionId()).isEqualTo(4L);
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.REJECTED);
        assertThat(asset.getStatus()).isNotEqualTo(AiGeneratedAssetStatus.APPROVED);
    }

    @Test
    void aiLogServiceIsPackagePrivate() {
        assertThat(Modifier.isPublic(AiLogService.class.getModifiers())).isFalse();
    }

    @Test
    void markGenerationRunningUpdatesQueuedJob() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-20T04:00:00+09:00");
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-20T04:01:00+09:00");
        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                createdAt
        );
        when(generationJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(generationJobRepository.save(any(AiGenerationJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiGenerationJob runningJob = aiLogService.markGenerationRunning(1L, startedAt);

        assertThat(runningJob.getStatus()).isEqualTo(AiGenerationJobStatus.RUNNING);
        assertThat(runningJob.getStartedAt()).isEqualTo(startedAt);
    }

    @Test
    void markGenerationSucceededUpdatesRunningJob() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-20T04:00:00+09:00");
        OffsetDateTime startedAt = OffsetDateTime.parse("2026-05-20T04:01:00+09:00");
        OffsetDateTime finishedAt = OffsetDateTime.parse("2026-05-20T04:02:00+09:00");
        AiGenerationJob job = AiGenerationJob.queue(
                AiGenerationJobType.EXPLANATION,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                createdAt
        );
        job.markRunning(startedAt);
        when(generationJobRepository.findById(1L)).thenReturn(Optional.of(job));
        when(generationJobRepository.save(any(AiGenerationJob.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiGenerationJob succeededJob = aiLogService.markGenerationSucceeded(1L, finishedAt);

        assertThat(succeededJob.getStatus()).isEqualTo(AiGenerationJobStatus.SUCCEEDED);
        assertThat(succeededJob.getFinishedAt()).isEqualTo(finishedAt);
    }

    @Test
    void markGenerationRunningThrowsWhenJobNotFound() {
        when(generationJobRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiLogService.markGenerationRunning(
                404L,
                OffsetDateTime.parse("2026-05-20T04:01:00+09:00")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_JOB_NOT_FOUND));
    }

    @Test
    void markGenerationSucceededThrowsWhenJobNotFound() {
        when(generationJobRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiLogService.markGenerationSucceeded(
                404L,
                OffsetDateTime.parse("2026-05-20T04:02:00+09:00")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_JOB_NOT_FOUND));
    }

    @Test
    void markGenerationFailedThrowsWhenJobNotFound() {
        when(generationJobRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiLogService.markGenerationFailed(
                404L,
                "LLM_TIMEOUT",
                OffsetDateTime.parse("2026-05-20T04:02:00+09:00")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_GENERATION_JOB_NOT_FOUND));
    }

    @Test
    void registerValidationLogThrowsWhenAssetNotFound() {
        when(generatedAssetRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> aiLogService.registerValidationLog(
                404L,
                1,
                AiValidationResult.PASSED,
                AiValidationReviewerType.AUTO,
                4L,
                "{\"result\":\"passed\"}",
                null,
                OffsetDateTime.parse("2026-05-20T04:04:00+09:00")
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AI_ASSET_NOT_FOUND));
    }

    @Test
    void passedValidationLogLeavesAssetValidating() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-20T04:03:00+09:00");
        OffsetDateTime validatedAt = OffsetDateTime.parse("2026-05-20T04:04:00+09:00");
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.QA_RESPONSE,
                AiTargetType.QA_REQUEST,
                700L,
                "{\"answer\":\"validated answer\"}",
                "QT-AI verified content",
                createdAt
        );
        when(generatedAssetRepository.findById(2L)).thenReturn(Optional.of(asset));
        when(validationLogRepository.save(any(AiValidationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiValidationLog log = aiLogService.registerValidationLog(
                2L,
                2,
                AiValidationResult.PASSED,
                AiValidationReviewerType.AUTO,
                4L,
                "{\"result\":\"passed\"}",
                null,
                validatedAt
        );

        assertThat(log.getResult()).isEqualTo(AiValidationResult.PASSED);
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        verify(generatedAssetRepository, never()).save(any(AiGeneratedAsset.class));
    }

    @Test
    void needsReviewValidationLogLeavesAssetValidating() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-20T04:03:00+09:00");
        OffsetDateTime validatedAt = OffsetDateTime.parse("2026-05-20T04:04:00+09:00");
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.QA_RESPONSE,
                AiTargetType.QA_REQUEST,
                700L,
                "{\"answer\":\"needs review answer\"}",
                "QT-AI verified content",
                createdAt
        );
        when(generatedAssetRepository.findById(2L)).thenReturn(Optional.of(asset));
        when(validationLogRepository.save(any(AiValidationLog.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AiValidationLog log = aiLogService.registerValidationLog(
                2L,
                2,
                AiValidationResult.NEEDS_REVIEW,
                AiValidationReviewerType.AUTO,
                4L,
                "{\"result\":\"needs_review\"}",
                null,
                validatedAt
        );

        assertThat(log.getResult()).isEqualTo(AiValidationResult.NEEDS_REVIEW);
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.VALIDATING);
        verify(generatedAssetRepository, never()).save(any(AiGeneratedAsset.class));
    }

    @Test
    void rejectedValidationLogRequiresValidatingAsset() {
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-05-20T04:03:00+09:00");
        OffsetDateTime approvedAt = OffsetDateTime.parse("2026-05-20T04:04:00+09:00");
        OffsetDateTime rejectedAt = OffsetDateTime.parse("2026-05-20T04:05:00+09:00");
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                1L,
                AiGeneratedAssetType.QA_RESPONSE,
                AiTargetType.QA_REQUEST,
                700L,
                "{\"answer\":\"already approved answer\"}",
                "QT-AI verified content",
                createdAt
        );
        asset.approve(approvedAt);
        when(generatedAssetRepository.findById(2L)).thenReturn(Optional.of(asset));

        assertThatThrownBy(() -> aiLogService.registerValidationLog(
                2L,
                2,
                AiValidationResult.REJECTED,
                AiValidationReviewerType.AUTO,
                4L,
                "{\"result\":\"rejected\"}",
                "POLICY_VIOLATION",
                rejectedAt
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        assertThat(asset.getStatus()).isEqualTo(AiGeneratedAssetStatus.APPROVED);
        verify(validationLogRepository, never()).save(any(AiValidationLog.class));
    }
}
