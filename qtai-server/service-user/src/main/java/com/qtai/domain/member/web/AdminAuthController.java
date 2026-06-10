package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.member.api.AdminLoginUseCase;
import com.qtai.domain.member.api.dto.AdminLoginRequest;
import com.qtai.domain.member.api.dto.AdminLoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 인증 컨트롤러. service-user가 유일한 JWT 발급자이므로 관리자 로그인도 이 서비스에 둔다.
 *
 * <p>{@code POST /api/v1/admin/auth/kakao} — 카카오 로그인(permitAll). admin-web(브라우저)이 카카오 토큰을 전달하면
 * 관리자 자격 검증 후 ADMIN 토큰을 발급한다. 다른 {@code /api/v1/admin/**}(관리자 비즈니스 API)은 SecurityConfig에서
 * denyAll(admin-server 책임)이며, 본 로그인 경로만 명시적으로 permitAll로 연다(CLAUDE.md §5, 계약 §1).
 */
@RestController
@RequestMapping("/api/v1/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminLoginUseCase adminLoginUseCase;

    /** POST /api/v1/admin/auth/kakao — 관리자 카카오 로그인. */
    @PostMapping("/kakao")
    public ResponseEntity<ApiResponse<AdminLoginResponse>> kakaoLogin(
            @Valid @RequestBody AdminLoginRequest request) {
        AdminLoginResponse response = adminLoginUseCase.adminLogin(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
