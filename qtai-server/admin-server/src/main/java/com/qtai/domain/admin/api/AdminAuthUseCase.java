package com.qtai.domain.admin.api;

import com.qtai.domain.admin.api.dto.AdminLoginResult;

/**
 * 관리자 자체 아이디/비밀번호 인증 UseCase 포트.
 *
 * <p>관리자 웹 로그인은 카카오가 아니라 username/password로 처리하며(2026-06-11 결정),
 * 성공 시 기존과 동일한 ADMIN RS256 토큰을 발급한다. 발급 이후의 권한 검증
 * (/api/v1/admin/**)은 변경 없이 그대로 동작한다.
 */
public interface AdminAuthUseCase {

    /** 아이디/비밀번호 로그인 → ADMIN access/refresh 발급. */
    AdminLoginResult login(String username, String rawPassword);

    /** refresh token으로 새 access/refresh 발급. */
    AdminLoginResult refresh(String refreshToken);
}
