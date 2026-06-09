package com.qtai.domain.ai.internal;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
class AdminAiBatchRunLogQueryRepository {

    private static final String LIST_FROM = """
            from AiBatchRunLog batchRunLog
            """;

    private static final String LIST_WHERE = """
            where (:batchName is null or batchRunLog.batchName = :batchName)
              and (:status is null or batchRunLog.status = :status)
              and (:fromCreatedAt is null or batchRunLog.createdAt >= :fromCreatedAt)
              and (:toCreatedAtExclusive is null or batchRunLog.createdAt < :toCreatedAtExclusive)
            """;

    private final EntityManager entityManager;

    AdminAiBatchRunLogQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    AdminAiBatchRunLogPage findAll(Filter filter, Pageable pageable) {
        List<Object[]> rows = entityManager.createQuery("""
                        select batchRunLog.id,
                               batchRunLog.batchName,
                               batchRunLog.status,
                               batchRunLog.createdCount,
                               batchRunLog.failedCount,
                               batchRunLog.processedCount,
                               batchRunLog.errorType,
                               batchRunLog.errorMessage,
                               batchRunLog.startedAt,
                               batchRunLog.finishedAt,
                               batchRunLog.createdAt
                        """ + LIST_FROM + LIST_WHERE + """
                        order by batchRunLog.createdAt desc, batchRunLog.id desc
                        """, Object[].class)
                .setParameter("batchName", filter.batchName())
                .setParameter("status", filter.status())
                .setParameter("fromCreatedAt", filter.fromCreatedAt())
                .setParameter("toCreatedAtExclusive", filter.toCreatedAtExclusive())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long totalElements = entityManager.createQuery("""
                        select count(batchRunLog.id)
                        """ + LIST_FROM + LIST_WHERE, Long.class)
                .setParameter("batchName", filter.batchName())
                .setParameter("status", filter.status())
                .setParameter("fromCreatedAt", filter.fromCreatedAt())
                .setParameter("toCreatedAtExclusive", filter.toCreatedAtExclusive())
                .getSingleResult();

        return new AdminAiBatchRunLogPage(rows.stream()
                .map(AdminAiBatchRunLogRow::from)
                .toList(), totalElements);
    }

    record Filter(
            AiBatchName batchName,
            AiBatchRunStatus status,
            LocalDateTime fromCreatedAt,
            LocalDateTime toCreatedAtExclusive
    ) {
    }

    record AdminAiBatchRunLogPage(
            List<AdminAiBatchRunLogRow> content,
            long totalElements
    ) {
    }

    record AdminAiBatchRunLogRow(
            Long id,
            AiBatchName batchName,
            AiBatchRunStatus status,
            int createdCount,
            int failedCount,
            int processedCount,
            String errorType,
            String errorMessage,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            LocalDateTime createdAt
    ) {

        private static AdminAiBatchRunLogRow from(Object[] row) {
            return new AdminAiBatchRunLogRow(
                    (Long) row[0],
                    (AiBatchName) row[1],
                    (AiBatchRunStatus) row[2],
                    ((Number) row[3]).intValue(),
                    ((Number) row[4]).intValue(),
                    ((Number) row[5]).intValue(),
                    (String) row[6],
                    (String) row[7],
                    (OffsetDateTime) row[8],
                    (OffsetDateTime) row[9],
                    (LocalDateTime) row[10]
            );
        }
    }
}
