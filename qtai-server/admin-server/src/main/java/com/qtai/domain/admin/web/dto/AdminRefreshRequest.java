package com.qtai.domain.admin.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 관리자 토큰 갱신 요청.
 *
 * @param refreshToken 발급받은 refresh token
 */
public record AdminRefreshRequest(
        @NotBlank String refreshToken
) {
}
