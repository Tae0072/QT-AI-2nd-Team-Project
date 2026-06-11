package com.qtai.domain.admin.api;

/**
 * 관리자 회원 조회 UseCase 포트.
 *
 * 일반 GetMemberUseCase와 달리 비활성/탈퇴 회원 포함 + 상세 정보(가입경로,
 * 최근 로그인, 신고 이력 등) 노출. ADMIN 권한 필수.
 */
public interface LookupMemberUseCase {

    // TODO: MemberAdminResponse lookupById(Long memberId);
    // TODO: Page<MemberAdminResponse> search(String keyword, MemberStatus status, Pageable pageable);
}
