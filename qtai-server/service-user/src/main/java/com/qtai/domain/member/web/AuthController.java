package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.member.api.LoginUseCase;
import com.qtai.domain.member.api.LogoutUseCase;
import com.qtai.domain.member.api.RefreshTokenUseCase;
import com.qtai.domain.member.api.dto.LoginRequest;
import com.qtai.domain.member.api.dto.LoginResponse;
import com.qtai.domain.member.api.dto.RefreshTokenRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 REST 엔드포인트.
 *
 * POST /api/v1/auth/kakao     - 카카오 로그인 (permitAll)
 * POST /api/v1/auth/refresh   - Access Token 재발급 (permitAll)
 * POST /api/v1/auth/logout    - 로그아웃 (authenticated)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;

    /**
     * POST /api/v1/auth/kakao - 카카오 로그인 (F-01).
     * SecurityConfig에서 permitAll 설정.
     */
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoLogin(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse response = loginUseCase.login(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/auth/refresh - Access Token 재발급.
     * SecurityConfig에서 permitAll 설정.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse response = refreshTokenUseCase.refresh(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * POST /api/v1/auth/logout - 로그아웃.
     * Refresh token을 Redis에서 삭제한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Long memberId) {
        logoutUseCase.logout(memberId);
        return ResponseEntity.noContent().build();
    }
}
