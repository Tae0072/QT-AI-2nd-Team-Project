package com.qtai.domain.mission.api;

/**
 * 회원 보존기간 만료 정리 — mission 도메인 데이터 삭제 포트.
 *
 * <p>탈퇴 후 2년(보존기간) 경과 회원의 미션 진행 데이터(member_mission_progress)를
 * hard delete한다. member 도메인의 보존기간 만료 배치(SYSTEM_BATCH)에서만 호출한다.
 */
public interface PurgeMemberMissionDataUseCase {

    /**
     * 해당 회원의 미션 진행 데이터를 모두 삭제한다 (호출자 트랜잭션에 참여).
     *
     * @param memberId 대상 회원 ID
     * @return 삭제된 행 수
     */
    int purgeByMemberId(Long memberId);
}
