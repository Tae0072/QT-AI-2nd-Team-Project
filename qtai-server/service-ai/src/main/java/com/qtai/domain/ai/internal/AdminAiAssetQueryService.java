package com.qtai.domain.ai.internal;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.asset.GetAdminAiAssetUseCase;
import com.qtai.domain.ai.api.admin.asset.ListAdminAiAssetsUseCase;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetDetailResponse;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListItem;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiAssetListResponse;
import com.qtai.domain.ai.api.admin.asset.dto.AdminAiValidationLogItem;
import com.qtai.domain.ai.api.admin.asset.dto.GetAdminAiAssetQuery;
import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;

@Service
public class AdminAiAssetQueryService implements ListAdminAiAssetsUseCase, GetAdminAiAssetUseCase {

    private static final String SORT = "createdAt,desc";
    private static final int MAX_PAGE_SIZE = 100;

    private final AdminAiAssetQueryRepository repository;
    private final ObjectMapper objectMapper;

    public AdminAiAssetQueryService(AdminAiAssetQueryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAiAssetListResponse listAdminAiAssets(ListAdminAiAssetsQuery query) {
        requireValidQuery(query);
        requireAuthorizedReviewer(query.memberRole(), query.adminRole());

        Pageable pageable = PageRequest.of(
                query.page(),
                query.size(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        AdminAiAssetQueryRepository.AdminAiAssetPage page = repository.findAll(query, pageable);
        int totalPages = totalPages(page.totalElements(), query.size());
        return new AdminAiAssetListResponse(
                page.content().stream()
                        .map(AdminAiAssetQueryService::toListItem)
                        .toList(),
                query.page(),
                query.size(),
                page.totalElements(),
                totalPages,
                query.page() == 0,
                totalPages == 0 || query.page() >= totalPages - 1,
                SORT
        );
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAiAssetDetailResponse getAdminAiAsset(GetAdminAiAssetQuery query) {
        requireValidQuery(query);
        requireAuthorizedReviewer(query.memberRole(), query.adminRole());

        AdminAiAssetQueryRepository.AdminAiAssetDetailRow detail = repository.findDetail(query.assetId())
                .orElseThrow(() -> new BusinessException(ErrorCode.AI_ASSET_NOT_FOUND));
        AdminAiAssetDetailResponse.GenerationJobSummary activeGenerationJob = repository.findActiveGenerationJob(
                        detail.jobType(),
                        detail.jobTargetType(),
                        detail.jobTargetId()
                )
                .map(AdminAiAssetQueryService::toGenerationJobSummary)
                .orElse(null);
        List<AdminAiValidationLogItem> validationLogs = repository.findValidationLogs(query.assetId()).stream()
                .map(AdminAiAssetQueryService::toValidationLogItem)
                .toList();

        return new AdminAiAssetDetailResponse(
                detail.id(),
                detail.assetType().name(),
                detail.targetType().name(),
                detail.targetId(),
                detail.status().name(),
                parsePayloadJson(detail.payloadJson()),
                detail.sourceLabel(),
                detail.createdAt(),
                detail.reviewedAt(),
                toGenerationJobSummary(detail),
                activeGenerationJob,
                new AdminAiAssetDetailResponse.PromptVersionSummary(
                        detail.promptVersionRowId(),
                        detail.promptType().name(),
                        detail.promptVersion(),
                        detail.promptVersionStatus().name()
                ),
                validationLogs
        );
    }

    private static AdminAiAssetDetailResponse.GenerationJobSummary toGenerationJobSummary(
            AdminAiAssetQueryRepository.AdminAiAssetDetailRow row
    ) {
        return new AdminAiAssetDetailResponse.GenerationJobSummary(
                row.generationJobId(),
                row.jobType().name(),
                row.jobTargetType().name(),
                row.jobTargetId(),
                row.promptVersionId(),
                row.jobStatus().name(),
                row.jobCreatedAt(),
                row.jobStartedAt(),
                row.jobFinishedAt(),
                row.jobErrorMessage()
        );
    }

    private static AdminAiAssetDetailResponse.GenerationJobSummary toGenerationJobSummary(
            AdminAiAssetQueryRepository.AdminAiGenerationJobRow row
    ) {
        return new AdminAiAssetDetailResponse.GenerationJobSummary(
                row.id(),
                row.jobType().name(),
                row.targetType().name(),
                row.targetId(),
                row.promptVersionId(),
                row.status().name(),
                row.createdAt(),
                row.startedAt(),
                row.finishedAt(),
                row.errorMessage()
        );
    }

    private JsonNode parsePayloadJson(String payloadJson) {
        try {
            JsonNode jsonNode = objectMapper.readTree(payloadJson);
            if (!jsonNode.isObject()) {
                throw new BusinessException(ErrorCode.INVALID_INPUT, "payloadJson is not a valid JSON object");
            }
            return jsonNode;
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "payloadJson is not a valid JSON object");
        }
    }

    private static AdminAiAssetListItem toListItem(AdminAiAssetQueryRepository.AdminAiAssetListRow row) {
        return new AdminAiAssetListItem(
                row.id(),
                row.assetType().name(),
                row.targetType().name(),
                row.targetId(),
                row.status().name(),
                new AdminAiAssetListItem.PromptVersionSummary(
                        row.promptVersionId(),
                        row.promptType().name(),
                        row.promptVersion(),
                        row.promptVersionStatus().name()
                ),
                row.checklistVersionId(),
                row.latestValidationResult() == null ? null : row.latestValidationResult().name(),
                row.sourceLabel() != null && !row.sourceLabel().isBlank(),
                row.createdAt()
        );
    }

    private static AdminAiValidationLogItem toValidationLogItem(
            AdminAiAssetQueryRepository.AdminAiValidationLogRow row
    ) {
        return new AdminAiValidationLogItem(
                row.validationLogId(),
                row.validationReferenceJobId(),
                row.checklistVersionId(),
                row.layer(),
                row.result().name(),
                row.reviewerType().name(),
                row.errorMessage(),
                row.createdAt()
        );
    }

    private static int totalPages(long totalElements, int size) {
        if (totalElements == 0) {
            return 0;
        }
        return (int) Math.ceil((double) totalElements / size);
    }

    private static void requireAuthorizedReviewer(String memberRole, String adminRole) {
        if (!"ADMIN".equals(memberRole) || !("REVIEWER".equals(adminRole) || "SUPER_ADMIN".equals(adminRole))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static void requireValidQuery(ListAdminAiAssetsQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.adminId(), "adminId");
        requireText(query.memberRole(), "memberRole");
        requireText(query.adminRole(), "adminRole");
        requirePositiveWhenPresent(query.promptVersionId(), "promptVersionId");
        requirePositiveWhenPresent(query.checklistVersionId(), "checklistVersionId");
        if (query.page() < 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "page must not be negative");
        }
        if (query.size() < 1 || query.size() > MAX_PAGE_SIZE) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "size must be between 1 and 100");
        }
    }

    private static void requireValidQuery(GetAdminAiAssetQuery query) {
        if (query == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "query must not be null");
        }
        requirePositive(query.adminId(), "adminId");
        requireText(query.memberRole(), "memberRole");
        requireText(query.adminRole(), "adminRole");
        requirePositive(query.assetId(), "assetId");
    }

    private static void requirePositive(Long value, String fieldName) {
        if (value == null || value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static void requirePositiveWhenPresent(Long value, String fieldName) {
        if (value != null && value <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must be positive");
        }
    }

    private static void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " must not be blank");
        }
    }
}
