package com.qtai.domain.mission.internal;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * 회원 미션 진행률 영속성 포트. Spring Data JPA로 구현.
 */
public interface MemberMissionProgressRepository extends JpaRepository<MemberMissionProgress, Long> {

    /** 회원의 진행률 목록을 집계 시작일 내림차순으로 조회 (대시보드 노출용). */
    List<MemberMissionProgress> findByMemberIdOrderByPeriodStartDateDesc(Long memberId);

    /** 멱등 upsert용 — (회원, 미션정의, 기간시작일) 단일 진행 레코드 조회. */
    Optional<MemberMissionProgress> findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(
            Long memberId, Long missionDefinitionId, LocalDate periodStartDate);

    /** 진행률이 존재하는(=이미 enroll된) 회원 ID 목록 — 배치 재계산 대상. */
    @Query("select distinct p.memberId from MemberMissionProgress p")
    List<Long> findDistinctMemberIds();
}
