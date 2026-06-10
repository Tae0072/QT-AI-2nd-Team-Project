package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.AdminLoginRequest;
import com.qtai.domain.member.api.dto.AdminLoginResponse;

/**
 * 관리자 카카오 로그인 UseCase.
 *
 * <p>사용자 로그인({@link LoginUseCase})과 동일하게 카카오 access token을 받아 검증하지만, 관리자 자격
 * (members.role=ADMIN + admin_users.admin_role)을 추가로 확인한 뒤에만 ADMIN 스코프 토큰을 발급한다(CLAUDE.md §5).
 * service-user가 유일한 JWT 발급자라 관리자 로그인도 이 서비스에 둔다(admin-server는 JWT를 발급하지 않는다).
 *
 * <p>계약: {@code doc/workspaces/DevD_이승욱/contracts/2026-06-10_admin-kakao-auth-api-contract.md}(합의 완료).
 */
public interface AdminLoginUseCase {

    /**
     * 카카오 access token으로 관리자 로그인을 수행한다.
     *
     * @param request 카카오 access token
     * @return ADMIN 토큰 + 관리자 요약 정보
     * @throws com.qtai.common.exception.BusinessException KAKAO_AUTH_FAILED(카카오 검증 실패),
     *         ADMIN_USER_NOT_FOUND(관리자 아님), ADMIN_USER_DISABLED(관리자 비활성), MEMBER_SUSPENDED(회원 정지)
     */
    AdminLoginResponse adminLogin(AdminLoginRequest request);
}
