package com.qtai.domain.admin.api;

/**
 * 운영 통계 조회 UseCase 포트.
 *
 * 관리자 대시보드에서 사용할 집계 데이터(가입자 수, QT 작성량, 신고 건수 등)를
 * 반환한다. 구현체는 AdminService.
 */
public interface GetStatsUseCase {

    // TODO: AdminStatsResponse getStats(LocalDate from, LocalDate to);
    //       기간별 핵심 지표 집계 (membersCount, qtCount, reportCount, activeUsers 등)
}
