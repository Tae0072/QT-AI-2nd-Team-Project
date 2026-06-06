package com.qtai.domain.ai.internal;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
class AiGenerationJobRunner {

    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private final AiGenerationJobRepository generationJobRepository;
    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final AiAutoValidationService aiAutoValidationService;
    private final Map<AiGenerationJobType, AiGenerationJobHandler> handlers;
    private final Clock clock;
    private final TransactionTemplate transactionTemplate;

    // 스프링 주입 생성자. 생성자가 2개라 @Autowired로 주입 대상을 명시하지 않으면
    // 스프링이 생성자를 못 고르고 기본 생성자를 찾다 기동에 실패한다.
    @Autowired
    AiGenerationJobRunner(
            AiGenerationJobRepository generationJobRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            AiAutoValidationService aiAutoValidationService,
            List<AiGenerationJobHandler> handlers,
            PlatformTransactionManager transactionManager
    ) {
        this(
                generationJobRepository,
                generatedAssetRepository,
                aiAutoValidationService,
                handlers,
                Clock.system(KST_ZONE),
                new TransactionTemplate(transactionManager)
        );
    }

    AiGenerationJobRunner(
            AiGenerationJobRepository generationJobRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            AiAutoValidationService aiAutoValidationService,
            List<AiGenerationJobHandler> handlers,
            Clock clock,
            TransactionTemplate transactionTemplate
    ) {
        this.generationJobRepository = generationJobRepository;
        this.generatedAssetRepository = generatedAssetRepository;
        this.aiAutoValidationService = aiAutoValidationService;
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

    /**
     * 고착된 RUNNING job을 FAILED로 풀어 재처리를 가능케 한다 (P1-3).
     *
     * <p>워커가 markRunning(commit) 후 완료 전에 크래시하면 job이 RUNNING으로 영구 고착되고
     * active_unique_key 때문에 재생성도 막힌다. timeout 임계 이전에 시작된 RUNNING을 FAILED로
     * 전이해 unique key를 풀고, 시딩/관리자 재생성으로 다시 큐잉되게 한다.
     *
     * @param timeoutMillis RUNNING 허용 최대 시간(ms)
     * @param batchSize     한 번에 회수할 최대 job 수
     * @return FAILED로 전이한 job 수
     */
    int sweepStaleRunningJobs(long timeoutMillis, int batchSize) {
        if (batchSize < 1) {
            return 0;
        }
        OffsetDateTime threshold = now().minusNanos(timeoutMillis * 1_000_000L);
        List<Long> staleIds = generationJobRepository.findStaleRunningJobIds(
                threshold, PageRequest.of(0, batchSize));
        int sweptCount = 0;
        for (Long jobId : staleIds) {
            boolean swept = Boolean.TRUE.equals(transactionTemplate.execute(status ->
                    generationJobRepository.findByIdAndStatus(jobId, AiGenerationJobStatus.RUNNING)
                            .map(job -> {
                                job.markFailed("RUNNING_TIMEOUT_SWEPT", now());
                                return true;
                            })
                            .orElse(false)
            ));
            if (swept) {
                log.warn("고착 RUNNING job을 FAILED로 회수. jobId={}", jobId);
                sweptCount++;
            }
        }
        return sweptCount;
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
        } catch (RuntimeException exception) {
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
            AiGeneratedAsset savedAsset = generatedAssetRepository.save(asset);
            validateAutomatically(savedAsset);
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

    private void validateAutomatically(AiGeneratedAsset asset) {
        if (asset.getAssetType() == AiGeneratedAssetType.EXPLANATION) {
            aiAutoValidationService.validateExplanationAsset(asset.getId(), now());
        }
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

    private static String failureMessage(RuntimeException exception) {
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
