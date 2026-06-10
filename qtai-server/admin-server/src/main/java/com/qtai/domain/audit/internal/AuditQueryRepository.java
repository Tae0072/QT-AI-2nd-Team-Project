package com.qtai.domain.audit.internal;

import java.time.OffsetDateTime;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
class AuditQueryRepository {

    private static final String LIST_FROM = """
            from AuditLog auditLog
            """;

    private static final String LIST_WHERE = """
            where (:actorType is null or auditLog.actorType = :actorType)
              and (:actorId is null or auditLog.actorId = :actorId)
              and auditLog.actionType in :actionTypes
              and auditLog.targetType = :targetType
              and (:targetId is null or auditLog.targetId = :targetId)
              and (:fromCreatedAt is null or auditLog.createdAt >= :fromCreatedAt)
              and (:toCreatedAtExclusive is null or auditLog.createdAt < :toCreatedAtExclusive)
            """;

    private final EntityManager entityManager;

    AuditQueryRepository(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    AuditLogPage findAll(Filter filter, Pageable pageable) {
        List<Object[]> rows = entityManager.createQuery("""
                        select auditLog.id,
                               auditLog.adminUserId,
                               auditLog.actorType,
                               auditLog.actorId,
                               auditLog.actorLabel,
                               auditLog.actionType,
                               auditLog.targetType,
                               auditLog.targetId,
                               auditLog.beforeJson,
                               auditLog.afterJson,
                               auditLog.createdAt
                        """ + LIST_FROM + LIST_WHERE + """
                        order by auditLog.createdAt desc, auditLog.id desc
                        """, Object[].class)
                .setParameter("actorType", filter.actorType())
                .setParameter("actorId", filter.actorId())
                .setParameter("actionTypes", filter.actionTypes())
                .setParameter("targetType", filter.targetType())
                .setParameter("targetId", filter.targetId())
                .setParameter("fromCreatedAt", filter.from())
                .setParameter("toCreatedAtExclusive", filter.to())
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        Long totalElements = entityManager.createQuery("""
                        select count(auditLog.id)
                        """ + LIST_FROM + LIST_WHERE, Long.class)
                .setParameter("actorType", filter.actorType())
                .setParameter("actorId", filter.actorId())
                .setParameter("actionTypes", filter.actionTypes())
                .setParameter("targetType", filter.targetType())
                .setParameter("targetId", filter.targetId())
                .setParameter("fromCreatedAt", filter.from())
                .setParameter("toCreatedAtExclusive", filter.to())
                .getSingleResult();

        return new AuditLogPage(rows.stream()
                .map(AuditLogRow::from)
                .toList(), totalElements);
    }

    List<DashboardAuditLogRow> findRecent(Pageable pageable) {
        List<Object[]> rows = entityManager.createQuery("""
                        select auditLog.id,
                               auditLog.adminUserId,
                               auditLog.actorType,
                               auditLog.actionType,
                               auditLog.targetType,
                               auditLog.targetId,
                               auditLog.createdAt
                        """ + LIST_FROM + """
                        order by auditLog.createdAt desc, auditLog.id desc
                        """, Object[].class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return rows.stream()
                .map(DashboardAuditLogRow::from)
                .toList();
    }

    record Filter(
            String actorType,
            Long actorId,
            List<String> actionTypes,
            String targetType,
            Long targetId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
    }

    record AuditLogPage(
            List<AuditLogRow> content,
            long totalElements
    ) {
    }

    record AuditLogRow(
            Long id,
            Long adminUserId,
            String actorType,
            Long actorId,
            String actorLabel,
            String actionType,
            String targetType,
            Long targetId,
            String beforeJson,
            String afterJson,
            OffsetDateTime createdAt
    ) {

        private static AuditLogRow from(Object[] row) {
            return new AuditLogRow(
                    (Long) row[0],
                    (Long) row[1],
                    (String) row[2],
                    (Long) row[3],
                    (String) row[4],
                    (String) row[5],
                    (String) row[6],
                    (Long) row[7],
                    (String) row[8],
                    (String) row[9],
                    (OffsetDateTime) row[10]
            );
        }
    }

    record DashboardAuditLogRow(
            Long id,
            Long adminUserId,
            String actorType,
            String actionType,
            String targetType,
            Long targetId,
            OffsetDateTime createdAt
    ) {

        private static DashboardAuditLogRow from(Object[] row) {
            return new DashboardAuditLogRow(
                    (Long) row[0],
                    (Long) row[1],
                    (String) row[2],
                    (String) row[3],
                    (String) row[4],
                    (Long) row[5],
                    (OffsetDateTime) row[6]
            );
        }
    }
}
