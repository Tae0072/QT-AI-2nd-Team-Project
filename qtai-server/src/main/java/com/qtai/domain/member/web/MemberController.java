package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.member.api.GetMemberSettingsUseCase;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.LoginUseCase;
import com.qtai.domain.member.api.LogoutUseCase;
import com.qtai.domain.member.api.UpdateMemberSettingsUseCase;
import com.qtai.domain.member.api.UpdateProfileUseCase;
import com.qtai.domain.member.api.WithdrawUseCase;
import com.qtai.domain.member.api.dto.LoginRequest;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.MemberSettingsResponse;
import com.qtai.domain.member.api.dto.MemberSettingsUpdateRequest;
import com.qtai.domain.member.api.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 REST 엔드포인트.
 *
 * base path: /api/v1/members  (로그인·로그아웃)
 * base path: /api/v1/me       (인증 필요 본인 API)
 *
 * 인증 없이 허용:
 *   POST /api/v1/auth/kakao        → LoginUseCase (CLAUDE.md §1, §5 — OAuth 경로 일원화)
 *   POST /api/v1/members/logout    → LogoutUseCase
 *
 * 인증 필요 (USER role):
 *   GET    /api/v1/members/{id}    → GetMemberUseCase (공개 항목만)
 *   GET    /api/v1/me              → GetMemberUseCase (본인 전체)
 *   PATCH  /api/v1/me              → UpdateProfileUseCase
 *   DELETE /api/v1/me              → WithdrawUseCase
 *   GET    /api/v1/me/settings     → GetMemberSettingsUseCase  (§4.1.6)
 *   PATCH  /api/v1/me/settings     → UpdateMemberSettingsUseCase (§4.1.7)
 */
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final LoginUseCase loginUseCase;
    private final LogoutUseCase logoutUseCase;
    private final GetMemberUseCase getMemberUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final GetMemberSettingsUseCase getMemberSettingsUseCase;
    private final UpdateMemberSettingsUseCase updateMemberSettingsUseCase;

    // -------------------------------------------------------------------------
    // 인증 (SecurityConfig에서 permitAll 처리)
    // CLAUDE.md §1, §5 — Flutter SDK 가 카카오 토큰 발급 후 /api/v1/auth/kakao 로 전달
    // -------------------------------------------------------------------------

    /**
     * POST /api/v1/auth/kakao — 카카오 로그인 (F-01).
     * SecurityConfig 에서 permitAll 설정 필요.
     */
    @PostMapping("/api/v1/auth/kakao")
    public ResponseEntity<ApiResponse<Object>> kakaoLogin(@RequestBody LoginRequest request) {
        // TODO: loginUseCase.login(request) 호출 후 LoginResponse 반환
        throw new UnsupportedOperationException("로그인 구현 예정 (F-01)");
    }

    /**
     * POST /api/v1/members/logout — 로그아웃 (F-02).
     * Authorization 헤더의 access token 을 blacklist 처리.
     */
    @PostMapping("/api/v1/members/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal Long memberId,
            @RequestHeader("Authorization") String authHeader) {
        // TODO: logoutUseCase.logout(memberId, bearerToken) 호출
        throw new UnsupportedOperationException("로그아웃 구현 예정 (F-02)");
    }

    // -------------------------------------------------------------------------
    // 회원 조회
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/me — 내 정보 조회 (F-03).
     */
    @GetMapping("/api/v1/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal Long memberId) {
        // TODO: getMemberUseCase.getMember(memberId)
        throw new UnsupportedOperationException("내 정보 조회 구현 예정 (F-03)");
    }

    /**
     * GET /api/v1/members/{id} — 타 회원 공개 정보 조회 (F-04).
     */
    @GetMapping("/api/v1/members/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(
            @PathVariable Long id) {
        // TODO: getMemberUseCase.getMember(id) — 공개 항목만 반환
        throw new UnsupportedOperationException("타 회원 조회 구현 예정 (F-04)");
    }

    // -------------------------------------------------------------------------
    // 회원 정보 수정 / 탈퇴
    // -------------------------------------------------------------------------

    /**
     * PATCH /api/v1/me — 내 프로필 수정 (F-05).
     * 닉네임 7일 변경 제한 검증은 UseCase 내부에서 처리한다.
     */
    @PatchMapping("/api/v1/me")
    public ResponseEntity<ApiResponse<MemberResponse>> updateProfile(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ProfileUpdateRequest request) {
        // TODO: updateProfileUseCase.updateProfile(memberId, request)
        throw new UnsupportedOperationException("프로필 수정 구현 예정 (F-05)");
    }

    /**
     * DELETE /api/v1/me — 회원 탈퇴 (F-06).
     * status=WITHDRAWN 전환 + 개인정보 익명화.
     */
    @DeleteMapping("/api/v1/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Long memberId) {
        // TODO: withdrawUseCase.withdraw(memberId, reason)
        throw new UnsupportedOperationException("회원 탈퇴 구현 예정 (F-06)");
    }

    // -------------------------------------------------------------------------
    // 사용자 설정 (API 명세서 §4.1.6 ~ §4.1.7)
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/me/settings — 사용자 설정 조회.
     */
    @GetMapping("/api/v1/me/settings")
    public ResponseEntity<ApiResponse<MemberSettingsResponse>> getSettings(
            @AuthenticationPrincipal Long memberId) {
        // TODO: getMemberSettingsUseCase.getSettings(memberId)
        throw new UnsupportedOperationException("설정 조회 구현 예정 (§4.1.6)");
    }

    /**
     * PATCH /api/v1/me/settings — 사용자 설정 수정 (Partial Update).
     */
    @PatchMapping("/api/v1/me/settings")
    public ResponseEntity<ApiResponse<MemberSettingsResponse>> updateSettings(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MemberSettingsUpdateRequest request) {
        // TODO: updateMemberSettingsUseCase.updateSettings(memberId, request)
        throw new UnsupportedOperationException("설정 수정 구현 예정 (§4.1.7)");
    }
}
