package com.qtai.domain.admin.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 아이디/비밀번호 로그인 요청.
 *
 * @param username 관리자 로그인 아이디
 * @param password 비밀번호(평문, 서버에서 BCrypt 검증 후 즉시 폐기 — 로그 금지)
 */
public record AdminLoginRequest(
        @NotBlank String username,
        @NotBlank String password
) {
}
