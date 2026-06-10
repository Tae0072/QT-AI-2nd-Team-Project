package com.qtai.domain.admin.api;

import com.qtai.domain.admin.api.dto.AdminDashboardResponse;

/**
 * 관리자 대시보드 조회 UseCase.
 */
public interface GetAdminDashboardUseCase {

    /**
     * AD-01 관리자 대시보드 요약을 조회한다.
     *
     * @param memberId JWT에서 추출한 관리자 회원 ID
     * @return 관리자 대시보드 응답
     */
    AdminDashboardResponse getDashboard(Long memberId);
}
