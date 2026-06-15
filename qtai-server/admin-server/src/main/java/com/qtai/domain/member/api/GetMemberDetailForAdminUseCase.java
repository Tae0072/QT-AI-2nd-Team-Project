package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.AdminMemberDetailResponse;

/**
 * 관리자 회원 상세 조회 UseCase 포트 (F-04/F-10).
 *
 * <p>GET /api/v1/admin/members/{memberId}/detail
 * 기본 정보 + 나눔/신고 집계를 묶어 운영 판단을 돕는다.
 */
public interface GetMemberDetailForAdminUseCase {

    AdminMemberDetailResponse getDetailForAdmin(Long memberId);
}
