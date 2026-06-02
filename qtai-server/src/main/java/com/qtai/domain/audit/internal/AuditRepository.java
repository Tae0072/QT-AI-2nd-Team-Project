package com.qtai.domain.audit.internal;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 감사 영속성 포트. Spring Data JPA로 구현.
 */
public interface AuditRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 관리자 조회용 필터 검색. 각 파라미터가 null이면 해당 조건은 무시한다.
     * 정렬은 호출자의 {@link Pageable}에 위임한다(기본 created_at desc).
     */
    @Query("select a from AuditLog a where "
            + "(:actorType is null or a.actorType = :actorType) and "
            + "(:actorId is null or a.actorId = :actorId) and "
            + "(:actionType is null or a.actionType = :actionType) and "
            + "(:from is null or a.createdAt >= :from) and "
            + "(:to is null or a.createdAt <= :to)")
    Page<AuditLog> search(@Param("actorType") String actorType,
                          @Param("actorId") Long actorId,
                          @Param("actionType") String actionType,
                          @Param("from") OffsetDateTime from,
                          @Param("to") OffsetDateTime to,
                          Pageable pageable);
}
