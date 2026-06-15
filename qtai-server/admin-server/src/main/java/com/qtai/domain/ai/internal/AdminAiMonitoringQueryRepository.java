package com.qtai.domain.ai.internal;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Repository;

@Repository
class AdminAiMonitoringQueryRepository {

    private final EntityManager entityManager;

    AdminAiMonitoringQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    Summary summarize(Filter filter) {
        return new Summary(
                countGenerationJobs(filter),
                countAssetStatuses(),
                countValidation(filter),
                findFailureReasons(filter),
                countBatchRuns(filter),
                findLatestBatchFailures(filter),
                findChecklists(filter)
        );
    }

    private GenerationJobCounts countGenerationJobs(Filter filter) {
        Map<AiGenerationJobStatus, Long> activeCounts = enumCountMap(AiGenerationJobStatus.class,
                entityManager.createQuery("""
                                select job.status, count(job.id)
                                from AiGenerationJob job
                                where job.status in :statuses
                                group by job.status
                                """, Object[].class)
                        .setParameter("statuses", List.of(
                                AiGenerationJobStatus.QUEUED,
                                AiGenerationJobStatus.RUNNING
                        ))
                        .getResultList());
        Map<AiGenerationJobStatus, Long> terminalCounts = enumCountMap(AiGenerationJobStatus.class,
                entityManager.createQuery("""
                                select job.status, count(job.id)
                                from AiGenerationJob job
                                where job.status in :statuses
                                  and job.finishedAt >= :fromAt
                                  and job.finishedAt < :toAtExclusive
                                group by job.status
                                """, Object[].class)
                        .setParameter("statuses", List.of(
                                AiGenerationJobStatus.SUCCEEDED,
                                AiGenerationJobStatus.FAILED
                        ))
                        .setParameter("fromAt", filter.fromAt())
                        .setParameter("toAtExclusive", filter.toAtExclusive())
                        .getResultList());

        return new GenerationJobCounts(
                activeCounts.getOrDefault(AiGenerationJobStatus.QUEUED, 0L),
                activeCounts.getOrDefault(AiGenerationJobStatus.RUNNING, 0L),
                terminalCounts.getOrDefault(AiGenerationJobStatus.SUCCEEDED, 0L),
                terminalCounts.getOrDefault(AiGenerationJobStatus.FAILED, 0L)
        );
    }

    private AssetStatusCounts countAssetStatuses() {
        Map<AiGeneratedAssetStatus, Long> counts = enumCountMap(AiGeneratedAssetStatus.class,
                entityManager.createQuery("""
                                select asset.status, count(asset.id)
                                from AiGeneratedAsset asset
                                group by asset.status
                                """, Object[].class)
                        .getResultList());

        return new AssetStatusCounts(
                counts.getOrDefault(AiGeneratedAssetStatus.VALIDATING, 0L),
                counts.getOrDefault(AiGeneratedAssetStatus.APPROVED, 0L),
                counts.getOrDefault(AiGeneratedAssetStatus.REJECTED, 0L),
                counts.getOrDefault(AiGeneratedAssetStatus.HIDDEN, 0L)
        );
    }

    private ValidationCounts countValidation(Filter filter) {
        Long waitingAssets = entityManager.createQuery("""
                        select count(asset.id)
                        from AiGeneratedAsset asset
                        where asset.status = :status
                        """, Long.class)
                .setParameter("status", AiGeneratedAssetStatus.VALIDATING)
                .getSingleResult();
        Map<AiGeneratedAssetStatus, Long> reviewedAssetCounts = enumCountMap(AiGeneratedAssetStatus.class,
                entityManager.createQuery("""
                                select asset.status, count(asset.id)
                                from AiGeneratedAsset asset
                                where asset.status in :statuses
                                  and asset.reviewedAt >= :fromAt
                                  and asset.reviewedAt < :toAtExclusive
                                group by asset.status
                                """, Object[].class)
                        .setParameter("statuses", List.of(
                                AiGeneratedAssetStatus.APPROVED,
                                AiGeneratedAssetStatus.REJECTED,
                                AiGeneratedAssetStatus.HIDDEN
                        ))
                        .setParameter("fromAt", filter.fromAt())
                        .setParameter("toAtExclusive", filter.toAtExclusive())
                        .getResultList());
        Map<AiValidationResult, Long> validationCounts = enumCountMap(AiValidationResult.class,
                entityManager.createQuery("""
                                select validationLog.result, count(validationLog.id)
                                from AiValidationLog validationLog
                                where validationLog.createdAt >= :fromAt
                                  and validationLog.createdAt < :toAtExclusive
                                group by validationLog.result
                                """, Object[].class)
                        .setParameter("fromAt", filter.fromAt())
                        .setParameter("toAtExclusive", filter.toAtExclusive())
                        .getResultList());

        return new ValidationCounts(
                waitingAssets,
                reviewedAssetCounts.getOrDefault(AiGeneratedAssetStatus.APPROVED, 0L),
                reviewedAssetCounts.getOrDefault(AiGeneratedAssetStatus.REJECTED, 0L),
                reviewedAssetCounts.getOrDefault(AiGeneratedAssetStatus.HIDDEN, 0L),
                validationCounts.getOrDefault(AiValidationResult.PASSED, 0L),
                validationCounts.getOrDefault(AiValidationResult.REJECTED, 0L),
                validationCounts.getOrDefault(AiValidationResult.NEEDS_REVIEW, 0L)
        );
    }

    private List<FailureReasonRow> findFailureReasons(Filter filter) {
        List<Object[]> rows = entityManager.createQuery("""
                        select case
                                 when validationLog.errorMessage is null
                                      or trim(validationLog.errorMessage) = ''
                                   then 'REJECTED'
                                 else validationLog.errorMessage
                               end,
                               count(validationLog.id)
                        from AiValidationLog validationLog
                        where validationLog.result = :result
                          and validationLog.createdAt >= :fromAt
                          and validationLog.createdAt < :toAtExclusive
                        group by case
                                   when validationLog.errorMessage is null
                                        or trim(validationLog.errorMessage) = ''
                                     then 'REJECTED'
                                   else validationLog.errorMessage
                                 end
                        order by count(validationLog.id) desc,
                                 case
                                   when validationLog.errorMessage is null
                                        or trim(validationLog.errorMessage) = ''
                                     then 'REJECTED'
                                   else validationLog.errorMessage
                                 end asc
                        """, Object[].class)
                .setParameter("result", AiValidationResult.REJECTED)
                .setParameter("fromAt", filter.fromAt())
                .setParameter("toAtExclusive", filter.toAtExclusive())
                .setMaxResults(10)
                .getResultList();

        return rows.stream()
                .map(row -> new FailureReasonRow((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    private BatchRunCounts countBatchRuns(Filter filter) {
        Map<AiBatchRunStatus, Long> counts = enumCountMap(AiBatchRunStatus.class,
                entityManager.createQuery("""
                                select batchRunLog.status, count(batchRunLog.id)
                                from AiBatchRunLog batchRunLog
                                where batchRunLog.createdAt >= :fromCreatedAt
                                  and batchRunLog.createdAt < :toCreatedAtExclusive
                                group by batchRunLog.status
                                """, Object[].class)
                        .setParameter("fromCreatedAt", filter.fromCreatedAt())
                        .setParameter("toCreatedAtExclusive", filter.toCreatedAtExclusive())
                        .getResultList());

        return new BatchRunCounts(
                counts.getOrDefault(AiBatchRunStatus.SUCCEEDED, 0L),
                counts.getOrDefault(AiBatchRunStatus.PARTIAL_FAILED, 0L),
                counts.getOrDefault(AiBatchRunStatus.FAILED, 0L)
        );
    }

    private List<BatchFailureRow> findLatestBatchFailures(Filter filter) {
        return entityManager.createQuery("""
                        select batchRunLog.id,
                               batchRunLog.batchName,
                               batchRunLog.status,
                               batchRunLog.errorType,
                               batchRunLog.errorMessage,
                               batchRunLog.createdAt
                        from AiBatchRunLog batchRunLog
                        where batchRunLog.status in :statuses
                          and batchRunLog.createdAt >= :fromCreatedAt
                          and batchRunLog.createdAt < :toCreatedAtExclusive
                        order by batchRunLog.createdAt desc, batchRunLog.id desc
                        """, Object[].class)
                .setParameter("statuses", List.of(AiBatchRunStatus.FAILED, AiBatchRunStatus.PARTIAL_FAILED))
                .setParameter("fromCreatedAt", filter.fromCreatedAt())
                .setParameter("toCreatedAtExclusive", filter.toCreatedAtExclusive())
                .setMaxResults(5)
                .getResultList()
                .stream()
                .map(BatchFailureRow::from)
                .toList();
    }

    private List<ChecklistRow> findChecklists(Filter filter) {
        List<ActiveChecklistRow> activeChecklists = entityManager.createQuery("""
                        select checklist.id,
                               checklist.checklistType,
                               checklist.version
                        from AiValidationChecklistVersion checklist
                        where checklist.status = :status
                        order by checklist.checklistType asc, checklist.id asc
                        """, Object[].class)
                .setParameter("status", AiValidationChecklistStatus.ACTIVE)
                .getResultList()
                .stream()
                .map(ActiveChecklistRow::from)
                .toList();
        if (activeChecklists.isEmpty()) {
            return List.of();
        }

        List<Long> checklistIds = activeChecklists.stream()
                .map(ActiveChecklistRow::id)
                .toList();
        Map<Long, ChecklistValidationCounts> countsByChecklistId = new HashMap<>();
        entityManager.createQuery("""
                        select validationLog.checklistVersionId,
                               validationLog.result,
                               count(validationLog.id)
                        from AiValidationLog validationLog
                        where validationLog.checklistVersionId in :checklistIds
                          and validationLog.createdAt >= :fromAt
                          and validationLog.createdAt < :toAtExclusive
                        group by validationLog.checklistVersionId, validationLog.result
                        """, Object[].class)
                .setParameter("checklistIds", checklistIds)
                .setParameter("fromAt", filter.fromAt())
                .setParameter("toAtExclusive", filter.toAtExclusive())
                .getResultList()
                .forEach(row -> {
                    Long checklistId = (Long) row[0];
                    AiValidationResult result = (AiValidationResult) row[1];
                    long count = ((Number) row[2]).longValue();
                    countsByChecklistId
                            .computeIfAbsent(checklistId, ignored -> new ChecklistValidationCounts())
                            .add(result, count);
                });

        return activeChecklists.stream()
                .map(checklist -> {
                    ChecklistValidationCounts counts = countsByChecklistId.getOrDefault(
                            checklist.id(),
                            new ChecklistValidationCounts()
                    );
                    return new ChecklistRow(
                            checklist.checklistType(),
                            checklist.version(),
                            counts.passCount(),
                            counts.totalCount()
                    );
                })
                .toList();
    }

    private static <E extends Enum<E>> Map<E, Long> enumCountMap(Class<E> enumType, List<Object[]> rows) {
        Map<E, Long> counts = new EnumMap<>(enumType);
        for (Object[] row : rows) {
            counts.put(enumType.cast(row[0]), ((Number) row[1]).longValue());
        }
        return counts;
    }

    record Filter(
            OffsetDateTime fromAt,
            OffsetDateTime toAtExclusive,
            LocalDateTime fromCreatedAt,
            LocalDateTime toCreatedAtExclusive
    ) {
    }

    record Summary(
            GenerationJobCounts generationJobs,
            AssetStatusCounts assetStatuses,
            ValidationCounts validation,
            List<FailureReasonRow> failureReasons,
            BatchRunCounts batchRuns,
            List<BatchFailureRow> latestBatchFailures,
            List<ChecklistRow> checklists
    ) {
    }

    record GenerationJobCounts(
            long queued,
            long running,
            long succeeded,
            long failed
    ) {
    }

    record AssetStatusCounts(
            long validating,
            long approved,
            long rejected,
            long hidden
    ) {
    }

    record ValidationCounts(
            long waitingAssets,
            long approvedAssets,
            long rejectedAssets,
            long hiddenAssets,
            long passCount,
            long failCount,
            long needsReviewCount
    ) {
    }

    record FailureReasonRow(
            String resultCode,
            long count
    ) {
    }

    record BatchRunCounts(
            long succeeded,
            long partialFailed,
            long failed
    ) {
    }

    record BatchFailureRow(
            Long id,
            AiBatchName batchName,
            AiBatchRunStatus status,
            String errorType,
            String errorMessage,
            LocalDateTime createdAt
    ) {

        private static BatchFailureRow from(Object[] row) {
            return new BatchFailureRow(
                    (Long) row[0],
                    (AiBatchName) row[1],
                    (AiBatchRunStatus) row[2],
                    (String) row[3],
                    (String) row[4],
                    (LocalDateTime) row[5]
            );
        }
    }

    record ChecklistRow(
            AiValidationChecklistType checklistType,
            String activeVersion,
            long passCount,
            long totalCount
    ) {
    }

    private record ActiveChecklistRow(
            Long id,
            AiValidationChecklistType checklistType,
            String version
    ) {

        private static ActiveChecklistRow from(Object[] row) {
            return new ActiveChecklistRow(
                    (Long) row[0],
                    (AiValidationChecklistType) row[1],
                    (String) row[2]
            );
        }
    }

    private static final class ChecklistValidationCounts {

        private long passCount;
        private long totalCount;

        void add(AiValidationResult result, long count) {
            if (result == AiValidationResult.PASSED) {
                passCount += count;
            }
            totalCount += count;
        }

        long passCount() {
            return passCount;
        }

        long totalCount() {
            return totalCount;
        }
    }
}
