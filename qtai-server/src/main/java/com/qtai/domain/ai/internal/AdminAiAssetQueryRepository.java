package com.qtai.domain.ai.internal;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.ai.api.dto.ListAdminAiAssetsQuery;

@Repository
public class AdminAiAssetQueryRepository {

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
            """ + LATEST_VALIDATION_JOIN;

    private static final String LIST_WHERE = """
            where (:assetType is null or asset.assetType = :assetType)
              and (:targetType is null or asset.targetType = :targetType)
              and (:status is null or asset.status = :status)
              and (:promptVersionId is null or job.promptVersionId = :promptVersionId)
              and (:checklistVersionId is null or latestValidation.checklistVersionId = :checklistVersionId)
            """;

    private final EntityManager entityManager;

    public AdminAiAssetQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public AdminAiAssetPage findAll(ListAdminAiAssetsQuery query, Pageable pageable) {
        List<Object[]> rows = entityManager.createQuery("""
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
                        """ + LIST_FROM + LIST_WHERE + """
                        order by asset.createdAt desc, asset.id desc
                        """, Object[].class)
                .setParameter("assetType", parseEnum(AiGeneratedAssetType.class, query.assetType(), "assetType"))
                .setParameter("targetType", parseEnum(AiTargetType.class, query.targetType(), "targetType"))
                .setParameter("status", parseEnum(AiGeneratedAssetStatus.class, query.status(), "status"))
                .setParameter("promptVersionId", query.promptVersionId())
                .setParameter("checklistVersionId", query.checklistVersionId())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long totalElements = entityManager.createQuery("""
                        select count(asset.id)
                        """ + LIST_FROM + LIST_WHERE, Long.class)
                .setParameter("assetType", parseEnum(AiGeneratedAssetType.class, query.assetType(), "assetType"))
                .setParameter("targetType", parseEnum(AiTargetType.class, query.targetType(), "targetType"))
                .setParameter("status", parseEnum(AiGeneratedAssetStatus.class, query.status(), "status"))
                .setParameter("promptVersionId", query.promptVersionId())
                .setParameter("checklistVersionId", query.checklistVersionId())
                .getSingleResult();

        return new AdminAiAssetPage(rows.stream()
                .map(AdminAiAssetListRow::from)
                .toList(), totalElements);
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
            Long checklistVersionId
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
                    (Long) row[12]
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
