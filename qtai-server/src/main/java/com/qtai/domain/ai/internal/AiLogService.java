package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;

@Service
public class AiLogService {

    private final AiGenerationJobRepository generationJobRepository;
    private final AiGeneratedAssetRepository generatedAssetRepository;
    private final AiValidationLogRepository validationLogRepository;

    public AiLogService(
            AiGenerationJobRepository generationJobRepository,
            AiGeneratedAssetRepository generatedAssetRepository,
            AiValidationLogRepository validationLogRepository
    ) {
        this.generationJobRepository = generationJobRepository;
        this.generatedAssetRepository = generatedAssetRepository;
        this.validationLogRepository = validationLogRepository;
    }

    @Transactional
    public AiGenerationJob queueGeneration(
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            String promptVersion,
            OffsetDateTime createdAt
    ) {
        AiGenerationJob job = AiGenerationJob.queue(jobType, targetType, targetId, promptVersion, createdAt);
        return generationJobRepository.save(job);
    }

    @Transactional
    public AiGenerationJob markGenerationRunning(Long jobId, OffsetDateTime startedAt) {
        AiGenerationJob job = findGenerationJob(jobId);
        job.markRunning(startedAt);
        return generationJobRepository.save(job);
    }

    @Transactional
    public AiGenerationJob markGenerationSucceeded(Long jobId, OffsetDateTime finishedAt) {
        AiGenerationJob job = findGenerationJob(jobId);
        job.markSucceeded(finishedAt);
        return generationJobRepository.save(job);
    }

    @Transactional
    public AiGenerationJob markGenerationFailed(Long jobId, String errorMessage, OffsetDateTime finishedAt) {
        AiGenerationJob job = findGenerationJob(jobId);
        job.markFailed(errorMessage, finishedAt);
        return generationJobRepository.save(job);
    }

    @Transactional
    public AiGeneratedAsset registerGeneratedAsset(
            Long generationJobId,
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            Long targetId,
            String promptVersion,
            String payloadJson,
            String sourceLabel,
            OffsetDateTime createdAt
    ) {
        AiGeneratedAsset asset = AiGeneratedAsset.create(
                generationJobId,
                assetType,
                targetType,
                targetId,
                promptVersion,
                payloadJson,
                sourceLabel,
                createdAt
        );
        return generatedAssetRepository.save(asset);
    }

    @Transactional
    public AiValidationLog registerValidationLog(
            Long assetId,
            int layer,
            AiValidationResult result,
            AiValidationReviewerType reviewerType,
            Long checklistVersionId,
            String checklistJson,
            String errorMessage,
            OffsetDateTime createdAt
    ) {
        AiGeneratedAsset asset = findGeneratedAsset(assetId);
        AiValidationLog log = AiValidationLog.create(
                assetId,
                layer,
                result,
                reviewerType,
                checklistVersionId,
                checklistJson,
                errorMessage,
                createdAt
        );
        AiValidationLog savedLog = validationLogRepository.save(log);
        if (result == AiValidationResult.REJECTED) {
            asset.reject(createdAt);
            generatedAssetRepository.save(asset);
        }
        return savedLog;
    }

    private AiGenerationJob findGenerationJob(Long jobId) {
        return generationJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "AI 생성 작업을 찾을 수 없습니다."));
    }

    private AiGeneratedAsset findGeneratedAsset(Long assetId) {
        return generatedAssetRepository.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "AI 산출물을 찾을 수 없습니다."));
    }
}
