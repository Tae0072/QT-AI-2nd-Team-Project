package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.GetMemberSettingsUseCase;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.UpdateMemberSettingsUseCase;
import com.qtai.domain.member.api.UpdateProfileUseCase;
import com.qtai.domain.member.api.WithdrawUseCase;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 회원 REST 엔드포인트.
 *
 * 인증 필요 (USER role):
 *   GET    /api/v1/members/{id}    -> GetMemberUseCase (공개 항목만)
 *   GET    /api/v1/me              -> GetMemberUseCase (본인 전체)
 *   PATCH  /api/v1/me              -> UpdateProfileUseCase
 *   DELETE /api/v1/me              -> WithdrawUseCase
 *   GET    /api/v1/me/settings     -> GetMemberSettingsUseCase
 *   PATCH  /api/v1/me/settings     -> UpdateMemberSettingsUseCase
 *
 * 인증 엔드포인트(login/logout/refresh)는 AuthController로 분리.
 */
@RestController
@RequiredArgsConstructor
public class MemberController {

    private final GetMemberUseCase getMemberUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final GetMemberSettingsUseCase getMemberSettingsUseCase;
    private final UpdateMemberSettingsUseCase updateMemberSettingsUseCase;

    // -------------------------------------------------------------------------
    // 회원 조회
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/me - 내 정보 조회 (F-03).
     */
    @GetMapping("/api/v1/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal Long memberId) {
        // TODO: getMemberUseCase.getMember(memberId) — Phase 5(mypage-api)에서 구현
        throw new UnsupportedOperationException(ErrorCode.NOT_IMPLEMENTED.getMessage());
    }

    /**
     * GET /api/v1/members/{id} - 타 회원 공개 정보 조회 (F-04).
     */
    @GetMapping("/api/v1/members/{id}")
    public ResponseEntity<ApiResponse<MemberResponse>> getMember(
            @PathVariable Long id) {
        // TODO: getMemberUseCase.getMember(id) — Phase 5에서 구현
        throw new UnsupportedOperationException(ErrorCode.NOT_IMPLEMENTED.getMessage());
    }

    // -------------------------------------------------------------------------
    // 회원 정보 수정 / 탈퇴
    // -------------------------------------------------------------------------

    /**
     * PATCH /api/v1/me - 내 프로필 수정 (F-05).
     */
    @PatchMapping("/api/v1/me")
    public ResponseEntity<ApiResponse<MemberResponse>> updateProfile(
            @AuthenticationPrincipal Long memberId,
            @RequestBody ProfileUpdateRequest request) {
        // TODO: updateProfileUseCase.updateProfile(memberId, request) — Phase 5에서 구현
        throw new UnsupportedOperationException(ErrorCode.NOT_IMPLEMENTED.getMessage());
    }

    /**
     * DELETE /api/v1/me - 회원 탈퇴 (F-06).
     */
    @DeleteMapping("/api/v1/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Long memberId) {
        // TODO: withdrawUseCase.withdraw(memberId) — Phase 5에서 구현
        throw new UnsupportedOperationException(ErrorCode.NOT_IMPLEMENTED.getMessage());
    }

    // -------------------------------------------------------------------------
    // 사용자 설정
    // -------------------------------------------------------------------------

    /**
     * GET /api/v1/me/settings - 사용자 설정 조회.
     */
    @GetMapping("/api/v1/me/settings")
    public ResponseEntity<ApiResponse<MemberSettingsResponse>> getSettings(
            @AuthenticationPrincipal Long memberId) {
        // TODO: getMemberSettingsUseCase.getSettings(memberId) — Phase 5에서 구현
        throw new UnsupportedOperationException(ErrorCode.NOT_IMPLEMENTED.getMessage());
    }

    /**
     * PATCH /api/v1/me/settings - 사용자 설정 수정.
     */
    @PatchMapping("/api/v1/me/settings")
    public ResponseEntity<ApiResponse<MemberSettingsResponse>> updateSettings(
            @AuthenticationPrincipal Long memberId,
            @RequestBody MemberSettingsUpdateRequest request) {
        // TODO: updateMemberSettingsUseCase.updateSettings(memberId, request) — Phase 5에서 구현
        throw new UnsupportedOperationException(ErrorCode.NOT_IMPLEMENTED.getMessage());
    }
}
