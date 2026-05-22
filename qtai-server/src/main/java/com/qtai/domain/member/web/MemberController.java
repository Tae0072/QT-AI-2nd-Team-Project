package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.UpdateProfileUseCase;
import com.qtai.domain.member.api.WithdrawUseCase;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.NicknameChangeRequest;
import com.qtai.domain.member.api.dto.ProfileUpdateRequest;
import com.qtai.domain.member.internal.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 REST 엔드포인트.
 *
 * Phase 3(mypage-api) 범위: 조회·프로필·닉네임·탈퇴.
 * 인증(로그인/로그아웃)은 auth-jwt 브랜치에서 구현.
 */
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final GetMemberUseCase getMemberUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final MemberService memberService;

    // ── 인증 (auth-jwt 브랜치에서 구현 예정) ──

    @PostMapping("/api/v1/auth/kakao")
    public ResponseEntity<ApiResponse<Object>> kakaoLogin(@RequestBody Object request) {
        throw new UnsupportedOperationException("로그인 구현 예정 — auth-jwt 브랜치");
    }

    @PostMapping("/api/v1/members/logout")
    public ResponseEntity<Void> logout() {
        throw new UnsupportedOperationException("로그아웃 구현 예정 — auth-jwt 브랜치");
    }

    // ── 회원 조회 ──

    /** GET /api/v1/me — 내 정보 조회. */
    @GetMapping("/api/v1/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal Long memberId) {
        MemberResponse response = getMemberUseCase.getMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/v1/members/{id} — 타 회원 공개 정보 조회. */
    @GetMapping("/api/v1/members/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(@PathVariable Long id) {
        MemberResponse response = getMemberUseCase.getMember(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── 프로필 수정 ──

    /** PATCH /api/v1/me — 프로필 수정. */
    @PatchMapping("/api/v1/me")
    public ResponseEntity<ApiResponse<MemberResponse>> updateProfile(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        MemberResponse response = updateProfileUseCase.updateProfile(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ── 닉네임 변경 (7일 잠금) ──

    /** PATCH /api/v1/me/nickname — 닉네임 변경. */
    @PatchMapping("/api/v1/me/nickname")
    public ResponseEntity<ApiResponse<MemberResponse>> changeNickname(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody NicknameChangeRequest request) {
        MemberResponse response = memberService.changeNickname(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/v1/me/nickname/available?nickname=xxx — 닉네임 사용 가능 확인. */
    @GetMapping("/api/v1/me/nickname/available")
    public ResponseEntity<ApiResponse<Boolean>> checkNicknameAvailable(
            @org.springframework.web.bind.annotation.RequestParam String nickname) {
        boolean available = memberService.isNicknameAvailable(nickname);
        return ResponseEntity.ok(ApiResponse.success(available));
    }

    // ── 회원 탈퇴 ──

    /** DELETE /api/v1/me — 회원 탈퇴. */
    @DeleteMapping("/api/v1/me")
    public ResponseEntity<Void> withdraw(@AuthenticationPrincipal Long memberId) {
        withdrawUseCase.withdraw(memberId, null);
        return ResponseEntity.noContent().build();
    }

    // ── 사용자 설정 (auth-jwt 브랜치와 병합 시 Settings UseCase 구현 추가) ──

    @GetMapping("/api/v1/me/settings")
    public ResponseEntity<ApiResponse<Object>> getSettings(
            @AuthenticationPrincipal Long memberId) {
        throw new UnsupportedOperationException("설정 조회 구현 예정");
    }

    @PatchMapping("/api/v1/me/settings")
    public ResponseEntity<ApiResponse<Object>> updateSettings(
            @AuthenticationPrincipal Long memberId,
            @RequestBody Object request) {
        throw new UnsupportedOperationException("설정 수정 구현 예정");
    }
}
