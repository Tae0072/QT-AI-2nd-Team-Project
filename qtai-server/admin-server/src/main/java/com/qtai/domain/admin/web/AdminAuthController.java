package com.qtai.domain.admin.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.admin.api.AdminAuthUseCase;
import com.qtai.domain.admin.api.dto.AdminLoginResult;
import com.qtai.domain.admin.web.dto.AdminLoginRequest;
import com.qtai.domain.admin.web.dto.AdminRefreshRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 인증 컨트롤러 (자체 아이디/비밀번호 로그인).
 *
 * <p>관리자 웹은 카카오가 아니라 아이디/비밀번호로 로그인한다(2026-06-11 결정).
 * 두 엔드포인트는 비인증 접근이 필요하므로 SecurityConfig에서 permitAll로 연다.
 * 다른 {@code /api/v1/admin/**}은 ROLE_ADMIN 필요.
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthUseCase adminAuthUseCase;

    /** POST /api/v1/admin/auth/login — 아이디/비밀번호 로그인. */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AdminLoginResult>> login(
            @Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResult result = adminAuthUseCase.login(request.username(), request.password());
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** POST /api/v1/admin/auth/refresh — refresh token으로 토큰 갱신. */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AdminLoginResult>> refresh(
            @Valid @RequestBody AdminRefreshRequest request) {
        AdminLoginResult result = adminAuthUseCase.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
