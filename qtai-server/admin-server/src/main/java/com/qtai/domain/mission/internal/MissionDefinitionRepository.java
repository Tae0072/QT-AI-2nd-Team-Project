package com.qtai.domain.mission.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 미션 정의 영속성 포트. Spring Data JPA로 구현.
 */
public interface MissionDefinitionRepository extends JpaRepository<MissionDefinition, Long> {

    /** 주어진 ID 목록의 미션 정의 조회 (진행률 → 정의 매핑용). */
    List<MissionDefinition> findByIdIn(List<Long> ids);

    /** 특정 상태의 미션 정의 조회 (배치 계산은 ACTIVE만 대상). */
    List<MissionDefinition> findByStatus(MissionDefinitionStatus status);

    /** 관리자 목록 — 전체 미션 정의를 id 순으로. */
    List<MissionDefinition> findAllByOrderByIdAsc();

    /** 코드 중복 검사(생성 시). */
    boolean existsByCode(String code);
}
