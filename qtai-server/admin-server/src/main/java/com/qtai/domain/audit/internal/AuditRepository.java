package com.qtai.domain.audit.internal;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 감사 영속성 포트. Spring Data JPA로 구현.
 */
public interface AuditRepository extends JpaRepository<AuditLog, Long> {
}
