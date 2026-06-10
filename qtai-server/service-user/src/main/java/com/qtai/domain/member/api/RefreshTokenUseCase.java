package com.qtai.domain.member.api;

import com.qtai.domain.member.api.dto.LoginResponse;
import com.qtai.domain.member.api.dto.RefreshTokenRequest;

/**
 * Access Token 재발급 UseCase 포트.
 *
 * Refresh token 검증 후 새로운 access/refresh token 쌍을 발급한다 (rotation).
 */
public interface RefreshTokenUseCase {

    LoginResponse refresh(RefreshTokenRequest request);
}
