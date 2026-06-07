package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.member.api.LoginWithKakaoCodeUseCase;
import com.qtai.domain.member.api.dto.KakaoCodeLoginRequest;
import com.qtai.domain.member.api.dto.LoginResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 카카오 웹 로그인 엔드포인트 (서버 OAuth, B안 · DRAFT).
 *
 * <p>POST /api/v1/auth/kakao/web — 웹에서 받은 인가 코드를 받아 JWT를 발급한다.
 * SecurityConfig에서 permitAll. (기존 {@code AuthController}는 변경하지 않는다.)
 *
 * <p><b>정책 충돌</b>: CLAUDE.md §1은 서버사이드 OAuth 미사용을 규정한다. 본 경로는 카카오
 * JS SDK의 access token 직접 발급 중단에 따른 불가피한 서버 코드 교환이며, 머지 전
 * 강사/Lead 검토가 필요하다. 근거: doc/workspaces/Lead_강태오/designs/2026-06-07_web-kakao-login-server-oauth_design.md
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class KakaoWebAuthController {

    private final LoginWithKakaoCodeUseCase loginWithKakaoCodeUseCase;

    /** POST /api/v1/auth/kakao/web — 웹 인가 코드 로그인. */
    @PostMapping("/kakao/web")
    public ResponseEntity<ApiResponse<LoginResponse>> kakaoWebLogin(
            @Valid @RequestBody KakaoCodeLoginRequest request) {
        LoginResponse response = loginWithKakaoCodeUseCase.loginWithKakaoCode(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
