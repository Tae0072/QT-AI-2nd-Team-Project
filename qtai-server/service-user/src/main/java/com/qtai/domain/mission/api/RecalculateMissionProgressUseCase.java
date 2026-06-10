package com.qtai.domain.mission.api;

/**
 * 미션 진행률 재계산 UseCase 포트.
 *
 * <p>진행률은 사용자 요청이 아니라 노트 활동 집계로 갱신된다(ERD §2.24). 수집 배치(04:00 KST)가
 * 이 UseCase를 호출해 진행률을 재계산·반영한다. 사용자 노출은 대시보드 조회
 * ({@link GetMemberMissionProgressUseCase})가 담당한다.
 */
public interface RecalculateMissionProgressUseCase {

    /**
     * 특정 회원의 ACTIVE 미션 진행률을 재계산하고 upsert한다(없으면 생성).
     * 현재는 MONTHLY 주기 미션만 계산한다(note 월간 집계 기준).
     *
     * @param memberId 대상 회원 ID
     */
    void recalculate(Long memberId);

    /**
     * 진행률이 이미 존재하는(enroll된) 모든 회원의 진행률을 재계산한다. 배치 진입점.
     */
    void recalculateAllEnrolled();
}
