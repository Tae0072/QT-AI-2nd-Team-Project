package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.admin.asset.dto.ListAdminAiAssetsQuery;

@Repository
public class AdminAiAssetQueryRepository {

    private static final List<AiGenerationJobStatus> ACTIVE_GENERATION_STATUSES = List.of(
            AiGenerationJobStatus.QUEUED,
            AiGenerationJobStatus.RUNNING
    );

    private static final String LATEST_VALIDATION_JOIN = """
            left join AiValidationLog latestValidation
              on latestValidation.id = (
                select max(latestCandidate.id)
                from AiValidationLog latestCandidate
                where latestCandidate.aiAssetId = asset.id
                  and latestCandidate.createdAt = (
                    select max(latestCreated.createdAt)
                    from AiValidationLog latestCreated
                    where latestCreated.aiAssetId = asset.id
                  )
              )
            """;

    private static final String LIST_FROM = """
            from AiGeneratedAsset asset
            join AiGenerationJob job on job.id = asset.generationJobId
            join AiPromptVersion promptVersion on promptVersion.id = job.promptVersionId
            """;

    private static final String LIST_FROM_WITH_LATEST_VALIDATION = LIST_FROM + LATEST_VALIDATION_JOIN;

    private static final String COUNT_FROM = """
            from AiGeneratedAsset asset
            join AiGenerationJob job on job.id = asset.generationJobId
            """;

    private static final String BASE_WHERE = """
            where (:assetType is null or asset.assetType = :assetType)
              and (:targetType is null or asset.targetType = :targetType)
              and (:status is null or asset.status = :status)
              and (:promptVersionId is null or job.promptVersionId = :promptVersionId)
            """;

    private static final String LIST_WHERE = BASE_WHERE + """
              and (:checklistVersionId is null or latestValidation.checklistVersionId = :checklistVersionId)
            """;

    private static final String CHECKLIST_VERSION_WHERE = """
              and latestValidation.checklistVersionId = :checklistVersionId
            """;

    private static final String COUNT_FROM_WITH_LATEST_VALIDATION = COUNT_FROM + LATEST_VALIDATION_JOIN;

    private static final String COUNT_WHERE_WITH_CHECKLIST_VERSION = BASE_WHERE + CHECKLIST_VERSION_WHERE;

    private final EntityManager entityManager;

    public AdminAiAssetQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public AdminAiAssetPage findAll(ListAdminAiAssetsQuery query, Pageable pageable) {
        AiGeneratedAssetType assetType = parseEnum(AiGeneratedAssetType.class, query.assetType(), "assetType");
        AiTargetType targetType = parseEnum(AiTargetType.class, query.targetType(), "targetType");
        AiGeneratedAssetStatus status = parseEnum(AiGeneratedAssetStatus.class, query.status(), "status");

        boolean hasChecklistVersionFilter = query.checklistVersionId() != null;
        List<AdminAiAssetListRow> content = hasChecklistVersionFilter
                ? findAllWithLatestValidation(query, pageable, assetType, targetType, status)
                : findAllPageThenLatestValidation(query, pageable, assetType, targetType, status);

        String countQueryString = """
                        select count(asset.id)
                        """ + countFrom(hasChecklistVersionFilter) + countWhere(hasChecklistVersionFilter);
        var countQuery = entityManager.createQuery(countQueryString, Long.class)
                .setParameter("assetType", assetType)
                .setParameter("targetType", targetType)
                .setParameter("status", status)
                .setParameter("promptVersionId", query.promptVersionId());
        if (hasChecklistVersionFilter) {
            countQuery.setParameter("checklistVersionId", query.checklistVersionId());
        }
        Long totalElements = countQuery.getSingleResult();

        return new AdminAiAssetPage(content, totalElements);
    }

    private List<AdminAiAssetListRow> findAllWithLatestValidation(
            ListAdminAiAssetsQuery query,
            Pageable pageable,
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            AiGeneratedAssetStatus status
    ) {
        List<AdminAiAssetListRow> pageRows = entityManager.createQuery("""
                        select asset.id,
                               asset.assetType,
                               asset.targetType,
                               asset.targetId,
                               asset.status,
                               asset.sourceLabel,
                               asset.createdAt,
                               promptVersion.id,
                               promptVersion.promptType,
                               promptVersion.version,
                               promptVersion.status,
                               latestValidation.result,
                               latestValidation.checklistVersionId
                        """ + LIST_FROM_WITH_LATEST_VALIDATION + LIST_WHERE + orderBy(status), Object[].class)
                .setParameter("assetType", assetType)
                .setParameter("targetType", targetType)
                .setParameter("status", status)
                .setParameter("promptVersionId", query.promptVersionId())
                .setParameter("checklistVersionId", query.checklistVersionId())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList()
                .stream()
                .map(AdminAiAssetListRow::from)
                .toList();
        return attachValidationSummaries(pageRows);
    }

    private List<AdminAiAssetListRow> findAllPageThenLatestValidation(
            ListAdminAiAssetsQuery query,
            Pageable pageable,
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            AiGeneratedAssetStatus status
    ) {
        List<AdminAiAssetListRow> pageRows = entityManager.createQuery("""
                        select asset.id,
                               asset.assetType,
                               asset.targetType,
                               asset.targetId,
                               asset.status,
                               asset.sourceLabel,
                               asset.createdAt,
                               promptVersion.id,
                               promptVersion.promptType,
                               promptVersion.version,
                               promptVersion.status
                        """ + LIST_FROM + BASE_WHERE + orderBy(status), Object[].class)
                .setParameter("assetType", assetType)
                .setParameter("targetType", targetType)
                .setParameter("status", status)
                .setParameter("promptVersionId", query.promptVersionId())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList()
                .stream()
                .map(AdminAiAssetListRow::fromPageRow)
                .toList();
        if (pageRows.isEmpty()) {
            return pageRows;
        }

        return attachValidationSummaries(pageRows);
    }

    private List<AdminAiAssetListRow> attachValidationSummaries(List<AdminAiAssetListRow> pageRows) {
        if (pageRows.isEmpty()) {
            return pageRows;
        }

        Map<Long, ValidationSummary> validationSummaries = findValidationSummaries(
                pageRows.stream().map(AdminAiAssetListRow::id).toList()
        );
        return pageRows.stream()
                .map(row -> row.withValidationSummary(validationSummaries.get(row.id())))
                .toList();
    }

    private static String countFrom(boolean hasChecklistVersionFilter) {
        return hasChecklistVersionFilter ? COUNT_FROM_WITH_LATEST_VALIDATION : COUNT_FROM;
    }

    private static String countWhere(boolean hasChecklistVersionFilter) {
        return hasChecklistVersionFilter ? COUNT_WHERE_WITH_CHECKLIST_VERSION : BASE_WHERE;
    }

    private static String orderBy(AiGeneratedAssetStatus status) {
        if (isReviewedStatus(status)) {
            return """
                    order by asset.reviewedAt desc, asset.id desc
                    """;
        }
        return """
                order by asset.createdAt desc, asset.id desc
                """;
    }

    private static boolean isReviewedStatus(AiGeneratedAssetStatus status) {
        return status == AiGeneratedAssetStatus.APPROVED
                || status == AiGeneratedAssetStatus.REJECTED
                || status == AiGeneratedAssetStatus.HIDDEN;
    }

    public Optional<AdminAiAssetDetailRow> findDetail(Long assetId) {
        List<Object[]> rows = entityManager.createQuery("""
                        select asset.id,
                               asset.assetType,
                               asset.targetType,
                               asset.targetId,
                               asset.status,
                               asset.payloadJson,
                               asset.sourceLabel,
                               asset.createdAt,
                               asset.reviewedAt,
                               job.id,
                               job.jobType,
                               job.targetType,
                               job.targetId,
                               job.promptVersionId,
                               job.status,
                               job.createdAt,
                               job.startedAt,
                               job.finishedAt,
                               job.errorMessage,
                               promptVersion.id,
                               promptVersion.promptType,
                               promptVersion.version,
                               promptVersion.status
                        from AiGeneratedAsset asset
                        join AiGenerationJob job on job.id = asset.generationJobId
                        join AiPromptVersion promptVersion on promptVersion.id = job.promptVersionId
                        where asset.id = :assetId
                        """, Object[].class)
                .setParameter("assetId", assetId)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(AdminAiAssetDetailRow.from(rows.get(0)));
    }

    public Optional<AdminAiGenerationJobRow> findActiveGenerationJob(
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId
    ) {
        List<Object[]> rows = entityManager.createQuery("""
                        select job.id,
                               job.jobType,
                               job.targetType,
                               job.targetId,
                               job.promptVersionId,
                               job.status,
                               job.createdAt,
                               job.startedAt,
                               job.finishedAt,
                               job.errorMessage
                        from AiGenerationJob job
                        where job.jobType = :jobType
                          and job.targetType = :targetType
                          and job.targetId = :targetId
                          and job.status in :statuses
                        order by job.createdAt desc, job.id desc
                        """, Object[].class)
                .setParameter("jobType", jobType)
                .setParameter("targetType", targetType)
                .setParameter("targetId", targetId)
                .setParameter("statuses", ACTIVE_GENERATION_STATUSES)
                .setMaxResults(1)
                .getResultList();
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(AdminAiGenerationJobRow.from(rows.get(0)));
    }

    public List<AdminAiValidationLogRow> findValidationLogs(Long assetId) {
        return entityManager.createQuery("""
                        select validationLog.id,
                               validationLog.validationReferenceJobId,
                               validationLog.checklistVersionId,
                               validationLog.layer,
                               validationLog.result,
                               validationLog.reviewerType,
                               validationLog.errorMessage,
                               validationLog.createdAt
                        from AiValidationLog validationLog
                        where validationLog.aiAssetId = :assetId
                        order by validationLog.createdAt desc, validationLog.id desc
                        """, Object[].class)
                .setParameter("assetId", assetId)
                .getResultList()
                .stream()
                .map(AdminAiValidationLogRow::from)
                .toList();
    }

    private Map<Long, ValidationSummary> findValidationSummaries(List<Long> assetIds) {
        List<Object[]> rows = entityManager.createQuery("""
                        select validationLog.aiAssetId,
                               validationLog.layer,
                               validationLog.reviewerType,
                               validationLog.result,
                               validationLog.checklistVersionId
                        from AiValidationLog validationLog
                        where validationLog.aiAssetId in :assetIds
                        order by validationLog.aiAssetId asc, validationLog.createdAt desc, validationLog.id desc
                        """, Object[].class)
                .setParameter("assetIds", assetIds)
                .getResultList();

        Map<Long, ValidationSummary> validationSummaries = new HashMap<>();
        for (Object[] row : rows) {
            Long aiAssetId = (Long) row[0];
            int layer = ((Number) row[1]).intValue();
            AiValidationReviewerType reviewerType = (AiValidationReviewerType) row[2];
            AiValidationResult result = (AiValidationResult) row[3];
            Long checklistVersionId = (Long) row[4];
            ValidationSummary previous = validationSummaries.getOrDefault(
                    aiAssetId,
                    ValidationSummary.empty(aiAssetId)
            );
            validationSummaries.put(
                    aiAssetId,
                    previous.withLog(layer, reviewerType, result, checklistVersionId)
            );
        }
        return validationSummaries;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, fieldName + " is not supported");
        }
    }

    public record AdminAiAssetPage(
            List<AdminAiAssetListRow> content,
            long totalElements
    ) {
    }

    public record AdminAiAssetListRow(
            Long id,
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            Long targetId,
            AiGeneratedAssetStatus status,
            String sourceLabel,
            OffsetDateTime createdAt,
            Long promptVersionId,
            AiPromptType promptType,
            String promptVersion,
            AiPromptVersionStatus promptVersionStatus,
            AiValidationResult latestValidationResult,
            Long checklistVersionId,
            AiValidationResult autoValidationResult,
            AiValidationResult advisorValidationResult
    ) {

        private static AdminAiAssetListRow from(Object[] row) {
            return new AdminAiAssetListRow(
                    (Long) row[0],
                    (AiGeneratedAssetType) row[1],
                    (AiTargetType) row[2],
                    (Long) row[3],
                    (AiGeneratedAssetStatus) row[4],
                    (String) row[5],
                    (OffsetDateTime) row[6],
                    (Long) row[7],
                    (AiPromptType) row[8],
                    (String) row[9],
                    (AiPromptVersionStatus) row[10],
                    (AiValidationResult) row[11],
                    (Long) row[12],
                    null,
                    null
            );
        }

        private static AdminAiAssetListRow fromPageRow(Object[] row) {
            return new AdminAiAssetListRow(
                    (Long) row[0],
                    (AiGeneratedAssetType) row[1],
                    (AiTargetType) row[2],
                    (Long) row[3],
                    (AiGeneratedAssetStatus) row[4],
                    (String) row[5],
                    (OffsetDateTime) row[6],
                    (Long) row[7],
                    (AiPromptType) row[8],
                    (String) row[9],
                    (AiPromptVersionStatus) row[10],
                    null,
                    null,
                    null,
                    null
            );
        }

        private AdminAiAssetListRow withValidationSummary(ValidationSummary validationSummary) {
            if (validationSummary == null) {
                return this;
            }
            return new AdminAiAssetListRow(
                    id,
                    assetType,
                    targetType,
                    targetId,
                    status,
                    sourceLabel,
                    createdAt,
                    promptVersionId,
                    promptType,
                    promptVersion,
                    promptVersionStatus,
                    validationSummary.latestValidationResult(),
                    validationSummary.checklistVersionId(),
                    validationSummary.autoValidationResult(),
                    validationSummary.advisorValidationResult()
            );
        }
    }

    private record ValidationSummary(
            Long aiAssetId,
            AiValidationResult latestValidationResult,
            Long checklistVersionId,
            AiValidationResult autoValidationResult,
            AiValidationResult advisorValidationResult
    ) {

        private static ValidationSummary empty(Long aiAssetId) {
            return new ValidationSummary(aiAssetId, null, null, null, null);
        }

        private ValidationSummary withLog(
                int layer,
                AiValidationReviewerType reviewerType,
                AiValidationResult result,
                Long checklistVersionId
        ) {
            AiValidationResult nextLatestValidationResult = latestValidationResult == null
                    ? result
                    : latestValidationResult;
            Long nextChecklistVersionId = latestValidationResult == null
                    ? checklistVersionId
                    : this.checklistVersionId;
            AiValidationResult nextAutoValidationResult = autoValidationResult;
            if (nextAutoValidationResult == null && layer == 1 && reviewerType == AiValidationReviewerType.AUTO) {
                nextAutoValidationResult = result;
            }
            AiValidationResult nextAdvisorValidationResult = advisorValidationResult;
            if (nextAdvisorValidationResult == null && layer == 2 && reviewerType == AiValidationReviewerType.ADVISOR) {
                nextAdvisorValidationResult = result;
            }
            return new ValidationSummary(
                    aiAssetId,
                    nextLatestValidationResult,
                    nextChecklistVersionId,
                    nextAutoValidationResult,
                    nextAdvisorValidationResult
            );
        }
    }

    public record AdminAiAssetDetailRow(
            Long id,
            AiGeneratedAssetType assetType,
            AiTargetType targetType,
            Long targetId,
            AiGeneratedAssetStatus status,
            String payloadJson,
            String sourceLabel,
            OffsetDateTime createdAt,
            OffsetDateTime reviewedAt,
            Long generationJobId,
            AiGenerationJobType jobType,
            AiTargetType jobTargetType,
            Long jobTargetId,
            Long promptVersionId,
            AiGenerationJobStatus jobStatus,
            OffsetDateTime jobCreatedAt,
            OffsetDateTime jobStartedAt,
            OffsetDateTime jobFinishedAt,
            String jobErrorMessage,
            Long promptVersionRowId,
            AiPromptType promptType,
            String promptVersion,
            AiPromptVersionStatus promptVersionStatus
    ) {

        private static AdminAiAssetDetailRow from(Object[] row) {
            return new AdminAiAssetDetailRow(
                    (Long) row[0],
                    (AiGeneratedAssetType) row[1],
                    (AiTargetType) row[2],
                    (Long) row[3],
                    (AiGeneratedAssetStatus) row[4],
                    (String) row[5],
                    (String) row[6],
                    (OffsetDateTime) row[7],
                    (OffsetDateTime) row[8],
                    (Long) row[9],
                    (AiGenerationJobType) row[10],
                    (AiTargetType) row[11],
                    (Long) row[12],
                    (Long) row[13],
                    (AiGenerationJobStatus) row[14],
                    (OffsetDateTime) row[15],
                    (OffsetDateTime) row[16],
                    (OffsetDateTime) row[17],
                    (String) row[18],
                    (Long) row[19],
                    (AiPromptType) row[20],
                    (String) row[21],
                    (AiPromptVersionStatus) row[22]
            );
        }
    }

    public record AdminAiGenerationJobRow(
            Long id,
            AiGenerationJobType jobType,
            AiTargetType targetType,
            Long targetId,
            Long promptVersionId,
            AiGenerationJobStatus status,
            OffsetDateTime createdAt,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            String errorMessage
    ) {

        private static AdminAiGenerationJobRow from(Object[] row) {
            return new AdminAiGenerationJobRow(
                    (Long) row[0],
                    (AiGenerationJobType) row[1],
                    (AiTargetType) row[2],
                    (Long) row[3],
                    (Long) row[4],
                    (AiGenerationJobStatus) row[5],
                    (OffsetDateTime) row[6],
                    (OffsetDateTime) row[7],
                    (OffsetDateTime) row[8],
                    (String) row[9]
            );
        }
    }

    public record AdminAiValidationLogRow(
            Long validationLogId,
            Long validationReferenceJobId,
            Long checklistVersionId,
            int layer,
            AiValidationResult result,
            AiValidationReviewerType reviewerType,
            String errorMessage,
            OffsetDateTime createdAt
    ) {

        private static AdminAiValidationLogRow from(Object[] row) {
            return new AdminAiValidationLogRow(
                    (Long) row[0],
                    (Long) row[1],
                    (Long) row[2],
                    ((Number) row[3]).intValue(),
                    (AiValidationResult) row[4],
                    (AiValidationReviewerType) row[5],
                    (String) row[6],
                    (OffsetDateTime) row[7]
            );
        }
    }
}
