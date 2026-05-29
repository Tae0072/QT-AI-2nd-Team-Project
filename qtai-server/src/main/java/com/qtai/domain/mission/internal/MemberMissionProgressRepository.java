package com.qtai.domain.mission.internal;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원 미션 진행률 영속성 포트. Spring Data JPA로 구현.
 */
public interface MemberMissionProgressRepository extends JpaRepository<MemberMissionProgress, Long> {

    /** 회원의 진행률 목록을 집계 시작일 내림차순으로 조회 (대시보드 노출용). */
    List<MemberMissionProgress> findByMemberIdOrderByPeriodStartDateDesc(Long memberId);
}
