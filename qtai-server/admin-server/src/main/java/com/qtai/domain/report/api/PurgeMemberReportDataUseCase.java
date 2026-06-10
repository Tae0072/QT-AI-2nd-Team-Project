package com.qtai.domain.report.api;

/**
 * 회원 보존기간 만료 정리 — report 도메인 데이터 삭제 포트.
 *
 * <p>탈퇴 후 2년(보존기간) 경과 회원이 접수한 신고(reports.reporter_member_id)를
 * hard delete한다. 타인이 접수한 신고는 접수자의 기록이므로 유지한다.
 * member 도메인의 보존기간 만료 배치(SYSTEM_BATCH)에서만 호출한다.
 */
public interface PurgeMemberReportDataUseCase {

    /**
     * 해당 회원이 접수한 신고 데이터를 모두 삭제한다 (호출자 트랜잭션에 참여).
     *
     * @param memberId 대상 회원 ID
     * @return 삭제된 행 수
     */
    int purgeByMemberId(Long memberId);
}
