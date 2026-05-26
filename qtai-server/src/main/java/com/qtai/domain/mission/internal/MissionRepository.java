package com.qtai.domain.mission.internal;

/**
 * 미션 영속성 포트. Spring Data JPA로 구현.
 */
public interface MissionRepository {

    // TODO: extends JpaRepository<Mission, Long>
    // TODO: List<Mission> findByMemberIdOrderByStartedAtDesc(Long memberId);
    // TODO: Optional<Mission> findByMemberIdAndTypeAndStatus(Long memberId, String type, MissionStatus status);
    //       동일 type 진행 중 미션 중복 시작 방지용
}
