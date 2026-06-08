package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

class AiGenerationJobRunnerTest {

    private static final Clock CLOCK = Clock.fixed(
            Instant.parse("2026-05-29T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );
    private static final OffsetDateTime CREATED_AT = OffsetDateTime.parse("2026-05-29T04:00:00+09:00");

    private AiGenerationJobRepository generationJobRepository;
    private AiGeneratedAssetRepository generatedAssetRepository;
    private AiAutoValidationService aiAutoValidationService;

    @BeforeEach
    void setUp() {
        generationJobRepository = mock(AiGenerationJobRepository.class);
        generatedAssetRepository = mock(AiGeneratedAssetRepository.class);
        aiAutoValidationService = mock(AiAutoValidationService.class);
    }

    @Test
    void explanationJobRunsAndStoresAssetThenSucceeds() {
        AiGenerationJob job = job(99L, AiGenerationJobType.EXPLANATION);
        AiGeneratedAsset asset = asset(job);
        AiGenerationJobRunner runner = runner(List.of(handler(AiGenerationJobType.EXPLANATION, asset)));

        when(generationJobRepository.findQueuedJobIds(any(), any())).thenReturn(List.of(99L));
        when(generationJobRepository.findByIdAndStatus(99L, AiGenerationJobStatus.QUEUED))
                .thenReturn(Optional.of(job));
        when(generationJobRepository.findById(99L)).thenReturn(Optional.of(job));
        when(generatedAssetRepository.save(asset)).thenReturn(asset);

        int processedCount = runner.runQueuedBatch(5);

        assertThat(processedCount).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.SUCCEEDED);
        assertThat(job.getStartedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThat(job.getFinishedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThat(job.getErrorMessage()).isNull();
        verify(generatedAssetRepository).save(asset);
        verify(aiAutoValidationService).validateExplanationAsset(500L, OffsetDateTime.now(CLOCK));
    }

    @Test
    void autoValidationErrorFailsJobAfterAssetGeneration() {
        AiGenerationJob job = job(102L, AiGenerationJobType.EXPLANATION);
        AiGeneratedAsset asset = asset(job);
        AiGenerationJobRunner runner = runner(List.of(handler(AiGenerationJobType.EXPLANATION, asset)));

        when(generationJobRepository.findByIdAndStatus(102L, AiGenerationJobStatus.QUEUED))
                .thenReturn(Optional.of(job));
        when(generationJobRepository.findById(102L)).thenReturn(Optional.of(job));
        when(generatedAssetRepository.save(asset)).thenReturn(asset);
        when(aiAutoValidationService.validateExplanationAsset(500L, OffsetDateTime.now(CLOCK)))
                .thenThrow(new BusinessException(ErrorCode.INTERNAL_ERROR, "AUTO_VALIDATION_CONFIGURATION_ERROR"));

        boolean processed = runner.runJob(102L);

        assertThat(processed).isTrue();
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("AUTO_VALIDATION_CONFIGURATION_ERROR");
    }

    @Test
    void llmTimeoutFailsJobWithSafeFailureCodeWithoutStoringAsset() {
        AiGenerationJob job = job(100L, AiGenerationJobType.EXPLANATION);
        AiGenerationJobRunner runner = runner(List.of(failingHandler(
                AiGenerationJobType.EXPLANATION,
                new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM_TIMEOUT")
        )));

        when(generationJobRepository.findByIdAndStatus(100L, AiGenerationJobStatus.QUEUED))
                .thenReturn(Optional.of(job));
        when(generationJobRepository.findById(100L)).thenReturn(Optional.of(job));

        boolean processed = runner.runJob(100L);

        assertThat(processed).isTrue();
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("LLM_TIMEOUT");
        verify(generatedAssetRepository, never()).save(any());
        verify(aiAutoValidationService, never()).validateExplanationAsset(any(), any());
    }

    @Test
    void llmRateLimitFailsJobWithSafeFailureCodeWithoutStoringAsset() {
        AiGenerationJob job = job(103L, AiGenerationJobType.EXPLANATION);
        AiGenerationJobRunner runner = runner(List.of(failingHandler(
                AiGenerationJobType.EXPLANATION,
                new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM_RATE_LIMIT")
        )));

        when(generationJobRepository.findByIdAndStatus(103L, AiGenerationJobStatus.QUEUED))
                .thenReturn(Optional.of(job));
        when(generationJobRepository.findById(103L)).thenReturn(Optional.of(job));

        boolean processed = runner.runJob(103L);

        assertThat(processed).isTrue();
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("LLM_RATE_LIMIT");
        verify(generatedAssetRepository, never()).save(any());
        verify(aiAutoValidationService, never()).validateExplanationAsset(any(), any());
    }

    @Test
    void llmProviderErrorFailsJobWithSafeFailureCodeWithoutStoringAsset() {
        AiGenerationJob job = job(104L, AiGenerationJobType.EXPLANATION);
        AiGenerationJobRunner runner = runner(List.of(failingHandler(
                AiGenerationJobType.EXPLANATION,
                new BusinessException(ErrorCode.INTERNAL_ERROR, "LLM_PROVIDER_ERROR")
        )));

        when(generationJobRepository.findByIdAndStatus(104L, AiGenerationJobStatus.QUEUED))
                .thenReturn(Optional.of(job));
        when(generationJobRepository.findById(104L)).thenReturn(Optional.of(job));

        boolean processed = runner.runJob(104L);

        assertThat(processed).isTrue();
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("LLM_PROVIDER_ERROR");
        verify(generatedAssetRepository, never()).save(any());
        verify(aiAutoValidationService, never()).validateExplanationAsset(any(), any());
    }

    @Test
    void simulatorJobFailsWithDisabledReasonWithoutCallingLlmPath() {
        AiGenerationJob job = job(101L, AiGenerationJobType.SIMULATOR);
        AiGenerationJobRunner runner = runner(List.of(new SimulatorGenerationJobHandler()));

        when(generationJobRepository.findByIdAndStatus(101L, AiGenerationJobStatus.QUEUED))
                .thenReturn(Optional.of(job));
        when(generationJobRepository.findById(101L)).thenReturn(Optional.of(job));

        boolean processed = runner.runJob(101L);

        assertThat(processed).isTrue();
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("SIMULATOR_GENERATION_DISABLED");
        verify(generatedAssetRepository, never()).save(any());
        verify(aiAutoValidationService, never()).validateExplanationAsset(any(), any());
    }

    private AiGenerationJobRunner runner(List<AiGenerationJobHandler> handlers) {
        return new AiGenerationJobRunner(
                generationJobRepository,
                generatedAssetRepository,
                aiAutoValidationService,
                handlers,
                CLOCK,
                new TransactionTemplate(new NoOpTransactionManager())
        );
    }

    private static AiGenerationJobHandler handler(AiGenerationJobType jobType, AiGeneratedAsset asset) {
        return new AiGenerationJobHandler() {
            @Override
            public AiGenerationJobType jobType() {
                return jobType;
            }

            @Override
            public AiGeneratedAsset generate(AiGenerationJob job, OffsetDateTime createdAt) {
                return asset;
            }
        };
    }

    private static AiGenerationJobHandler failingHandler(AiGenerationJobType jobType, RuntimeException exception) {
        return new AiGenerationJobHandler() {
            @Override
            public AiGenerationJobType jobType() {
                return jobType;
            }

            @Override
            public AiGeneratedAsset generate(AiGenerationJob job, OffsetDateTime createdAt) {
                throw exception;
            }
        };
    }

    @Test
    void sweepStaleRunningJobs_고착_RUNNING을_FAILED로_회수한다() {
        // P1-3: markRunning 후 완료 전 크래시로 RUNNING 고착된 job을 FAILED로 풀어 재처리 가능케 함
        AiGenerationJob job = job(700L, AiGenerationJobType.EXPLANATION);
        job.markRunning(OffsetDateTime.now(CLOCK).minusHours(1)); // 1시간 전 시작 → 고착
        AiGenerationJobRunner runner = runner(List.of());

        when(generationJobRepository.findStaleRunningJobIds(any(), any())).thenReturn(List.of(700L));
        when(generationJobRepository.findByIdAndStatus(700L, AiGenerationJobStatus.RUNNING))
                .thenReturn(Optional.of(job));

        int swept = runner.sweepStaleRunningJobs(300_000L, 5);

        assertThat(swept).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("RUNNING_TIMEOUT_SWEPT");
    }

    @Test
    void sweepStaleRunningJobs_고착_없으면_0() {
        AiGenerationJobRunner runner = runner(List.of());
        when(generationJobRepository.findStaleRunningJobIds(any(), any())).thenReturn(List.of());

        assertThat(runner.sweepStaleRunningJobs(300_000L, 5)).isZero();
    }

    private static AiGenerationJob job(Long id, AiGenerationJobType jobType) {
        AiGenerationJob job = AiGenerationJob.queue(
                jobType,
                AiTargetType.QT_PASSAGE,
                35L,
                3L,
                CREATED_AT
        );
        setId(job, id);
        return job;
    }

    private static AiGeneratedAsset asset(AiGenerationJob job) {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                job.getId(),
                AiGeneratedAssetType.EXPLANATION,
                job.getTargetType(),
                job.getTargetId(),
                "{\"explanations\":[],\"glossaryTerms\":[]}",
                "QT-AI DeepSeek",
                OffsetDateTime.now(CLOCK)
        );
        setId(asset, 500L);
        return asset;
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

    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
