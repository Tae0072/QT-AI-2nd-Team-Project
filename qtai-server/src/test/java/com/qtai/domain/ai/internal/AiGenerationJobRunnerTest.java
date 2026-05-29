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

    @BeforeEach
    void setUp() {
        generationJobRepository = mock(AiGenerationJobRepository.class);
        generatedAssetRepository = mock(AiGeneratedAssetRepository.class);
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

        int processedCount = runner.runQueuedBatch(5);

        assertThat(processedCount).isEqualTo(1);
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.SUCCEEDED);
        assertThat(job.getStartedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThat(job.getFinishedAt()).isEqualTo(OffsetDateTime.now(CLOCK));
        assertThat(job.getErrorMessage()).isNull();
        verify(generatedAssetRepository).save(asset);
    }

    @Test
    void llmErrorFailsJobWithoutStoringAsset() {
        AiGenerationJob job = job(100L, AiGenerationJobType.EXPLANATION);
        AiGenerationJobRunner runner = runner(List.of(failingHandler(
                AiGenerationJobType.EXPLANATION,
                new BusinessException(ErrorCode.INTERNAL_ERROR, "DeepSeek API request failed")
        )));

        when(generationJobRepository.findByIdAndStatus(100L, AiGenerationJobStatus.QUEUED))
                .thenReturn(Optional.of(job));
        when(generationJobRepository.findById(100L)).thenReturn(Optional.of(job));

        boolean processed = runner.runJob(100L);

        assertThat(processed).isTrue();
        assertThat(job.getStatus()).isEqualTo(AiGenerationJobStatus.FAILED);
        assertThat(job.getErrorMessage()).isEqualTo("DeepSeek API request failed");
        verify(generatedAssetRepository, never()).save(any());
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
    }

    private AiGenerationJobRunner runner(List<AiGenerationJobHandler> handlers) {
        return new AiGenerationJobRunner(
                generationJobRepository,
                generatedAssetRepository,
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
        return AiGeneratedAsset.create(
                job.getId(),
                AiGeneratedAssetType.EXPLANATION,
                job.getTargetType(),
                job.getTargetId(),
                "{\"explanations\":[],\"glossaryTerms\":[]}",
                "QT-AI DeepSeek",
                OffsetDateTime.now(CLOCK)
        );
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
