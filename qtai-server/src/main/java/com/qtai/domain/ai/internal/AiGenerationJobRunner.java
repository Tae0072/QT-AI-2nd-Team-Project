package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Service
class AiGenerationJobRunner {

    private final AiGenerationJobRepository generationJobRepository;
    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final Map<AiGenerationJobType, AiGenerationJobHandler> handlers;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    AiGenerationJobRunner(
            AiGenerationJobRepository generationJobRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            List<AiGenerationJobHandler> handlers,
            PlatformTransactionManager transactionManager
    ) {
        this(
                generationJobRepository,
                generatedAssetRepository,
                handlers,
                Clock.systemDefaultZone(),
                new TransactionTemplate(transactionManager)
        );
    }

    AiGenerationJobRunner(
            AiGenerationJobRepository generationJobRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            List<AiGenerationJobHandler> handlers,
            Clock clock,
            TransactionTemplate transactionTemplate
    ) {
        this.generationJobRepository = generationJobRepository;
        this.generatedAssetRepository = generatedAssetRepository;
        this.handlers = registerHandlers(handlers);
        this.clock = clock;
        this.transactionTemplate = transactionTemplate;
    }

    int runQueuedBatch(int batchSize) {
        if (batchSize < 1) {
            return 0;
        }

        List<Long> jobIds = generationJobRepository.findQueuedJobIds(
                AiGenerationJobStatus.QUEUED,
                PageRequest.of(0, batchSize)
        );
        int processedCount = 0;
        for (Long jobId : jobIds) {
            if (runJob(jobId)) {
                processedCount++;
            }
        }
        return processedCount;
    }

    boolean runJob(Long jobId) {
        Optional<AiGenerationJob> claimedJob = claimQueuedJob(jobId);
        if (claimedJob.isEmpty()) {
            return false;
        }

        AiGenerationJob job = claimedJob.get();
        try {
            AiGeneratedAsset asset = requireAsset(job, handler(job.getJobType()).generate(job, now()));
            completeSucceeded(job.getId(), asset);
        } catch (Exception exception) {
            completeFailed(job.getId(), failureMessage(exception));
        }
        return true;
    }

    private Optional<AiGenerationJob> claimQueuedJob(Long jobId) {
        return transactionTemplate.execute(status ->
                generationJobRepository.findByIdAndStatus(jobId, AiGenerationJobStatus.QUEUED)
                        .map(job -> {
                            job.markRunning(now());
                            return job;
                        })
        );
    }

    private void completeSucceeded(Long jobId, AiGeneratedAsset asset) {
        transactionTemplate.executeWithoutResult(status -> {
            generatedAssetRepository.save(asset);
            findGenerationJob(jobId).markSucceeded(now());
        });
    }

    private void completeFailed(Long jobId, String errorMessage) {
        transactionTemplate.executeWithoutResult(status ->
                findGenerationJob(jobId).markFailed(errorMessage, now())
        );
    }

    private AiGenerationJob findGenerationJob(Long jobId) {
        return generationJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_GENERATION_JOB_NOT_FOUND));
    }

    private AiGenerationJobHandler handler(AiGenerationJobType jobType) {
        AiGenerationJobHandler handler = handlers.get(jobType);
        if (handler == null) {
            throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "AI_GENERATION_HANDLER_NOT_FOUND");
        }
        return handler;
    }

    private AiGeneratedAsset requireAsset(AiGenerationJob job, AiGeneratedAsset asset) {
        if (asset == null) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI_GENERATION_HANDLER_EMPTY_RESULT");
        }
        if (!job.getId().equals(asset.getGenerationJobId())) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI_GENERATION_ASSET_JOB_MISMATCH");
        }
        return asset;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }

    private static Map<AiGenerationJobType, AiGenerationJobHandler> registerHandlers(
            List<AiGenerationJobHandler> handlers
    ) {
        Map<AiGenerationJobType, AiGenerationJobHandler> registry = new EnumMap<>(AiGenerationJobType.class);
        for (AiGenerationJobHandler handler : handlers) {
            AiGenerationJobHandler previous = registry.put(handler.jobType(), handler);
            if (previous != null) {
                throw new IllegalStateException("Duplicate AI generation handler: " + handler.jobType());
            }
        }
        return Map.copyOf(registry);
    }

    private static String failureMessage(Exception exception) {
        if (exception instanceof BusinessException businessException
                && businessException.getMessage() != null
                && !businessException.getMessage().isBlank()) {
            return truncate(businessException.getMessage(), 200);
        }
        return exception.getClass().getSimpleName();
    }

    private static String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
