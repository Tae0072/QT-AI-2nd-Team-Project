package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.AdminMemberResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * 관리자 회원 목록·검색 UseCase 포트 (F-04/F-10).
 *
 * <p>GET /api/v1/admin/members?status=&q=&page=&size=
 * status가 null이면 전체 상태, q가 null이면 닉네임 필터를 건너뛴다.
 */
public interface ListMembersForAdminUseCase {

    Page<AdminMemberResponse> listForAdmin(String status, String q, Pageable pageable);

    AdminMemberResponse getForAdmin(Long memberId);
}
