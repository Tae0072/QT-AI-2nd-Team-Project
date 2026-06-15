package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.AdminMemberResponse;
import com.qtai.domain.member.api.dto.MemberStatusUpdateRequest;

/**
 * 관리자 회원 상태 변경 UseCase 포트 (F-10).
 *
 * <p>PATCH /api/v1/admin/members/{memberId}/status
 * ACTIVE(정지 해제) / SUSPENDED(정지) 전환만 허용한다.
 */
public interface UpdateMemberStatusForAdminUseCase {

    AdminMemberResponse updateStatus(Long memberId, MemberStatusUpdateRequest request);
}
