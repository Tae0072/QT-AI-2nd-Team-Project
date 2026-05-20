package com.qtai.domain.audit.internal;

/**
 * 감사 영속성 포트. Spring Data JPA로 구현.
 */
public interface AuditRepository {

    // TODO: extends JpaRepository<AuditLog, Long>
    // TODO: Page<AuditLog> findByFilters(Long actorId, String action, LocalDateTime from, LocalDateTime to, Pageable pageable);
    //       동적 쿼리 — Specification 또는 QueryDSL 권장
}
