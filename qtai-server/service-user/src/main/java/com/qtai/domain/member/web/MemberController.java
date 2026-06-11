package com.qtai.domain.member.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.member.api.GetMemberUseCase;
import com.qtai.domain.member.api.UpdateProfileUseCase;
import com.qtai.domain.member.api.WithdrawUseCase;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.member.api.dto.MemberResponse;
import com.qtai.domain.member.api.dto.NicknameChangeRequest;
import com.qtai.domain.member.api.dto.ProfileUpdateRequest;
import com.qtai.domain.member.api.dto.WithdrawRequest;
import com.qtai.domain.member.api.ChangeNicknameUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 회원 REST 엔드포인트.
 *
 * <p>Phase 3(mypage-api) 에서 구현.
 * <p>인증(로그인/로그아웃) 은 AuthController 로 분리.
 */
@RestController
@RequiredArgsConstructor
@Validated
public class MemberController {

    private final GetMemberUseCase getMemberUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;
    private final WithdrawUseCase withdrawUseCase;
    private final ChangeNicknameUseCase changeNicknameUseCase;

    // ── 회원 조회 ──

    /** GET /api/v1/me — 내 정보 조회. */
    @GetMapping("/api/v1/me")
    public ResponseEntity<ApiResponse<MemberResponse>> getMyInfo(
            @AuthenticationPrincipal Long memberId) {
        MemberResponse response = getMemberUseCase.getMember(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/v1/members/{id} — 타 회원 공개 프로필 조회 (비공개 필드 제외). */
    @GetMapping("/api/v1/members/{id}")
    public ResponseEntity<ApiResponse<MemberPublicResponse>> getMember(@PathVariable("id") Long id) {
        MemberPublicResponse response = getMemberUseCase.getMemberPublic(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/members?ids=1,2,3 — 활성 회원 공개 프로필 일괄 조회.
     *
     * <p>서비스 간 호출(예: service-note sharing/댓글 작성자 닉네임 N+1 방지)용. 탈퇴·정지 회원은
     * 결과에서 제외되며(요청 순서 보장 없음), 호출자는 누락 id에 자체 폴백 정책을 적용한다
     * ({@link GetMemberUseCase#getActivePublicProfiles}).
     */
    @GetMapping("/api/v1/members")
    public ResponseEntity<ApiResponse<List<MemberPublicResponse>>> getMembers(@RequestParam("ids") List<Long> ids) {
        List<MemberPublicResponse> response = getMemberUseCase.getActivePublicProfiles(ids);
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

    // ── 닉네임 변경 (즉시 변경 가능 — 2026-06-11 잠금 폐지) ──

    /** PATCH /api/v1/me/nickname — 닉네임 변경. */
    @PatchMapping("/api/v1/me/nickname")
    public ResponseEntity<ApiResponse<MemberResponse>> changeNickname(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody NicknameChangeRequest request) {
        MemberResponse response = changeNicknameUseCase.changeNickname(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * GET /api/v1/me/nickname/available?nickname=xxx — 닉네임 사용가능 여부.
     *
     * <p>열거 방어: /api/v1/me/** 경로이므로 인증 필수.
     * Rate limiting 은 향후 RateLimiter 어노테이션 추가 예정.
     */
    @GetMapping("/api/v1/me/nickname/available")
    public ResponseEntity<ApiResponse<Boolean>> checkNicknameAvailable(
            @AuthenticationPrincipal Long memberId,
            @RequestParam @NotBlank @Size(min = 2, max = 20) String nickname) {
        boolean available = changeNicknameUseCase.isNicknameAvailable(nickname);
        return ResponseEntity.ok(ApiResponse.success(available));
    }

    // ── 회원 탈퇴 ──

    /** DELETE /api/v1/me — 회원 탈퇴 (reason 은 감사 기록용, 선택). */
    @DeleteMapping("/api/v1/me")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody(required = false) WithdrawRequest request) {
        String reason = (request != null) ? request.reason() : null;
        withdrawUseCase.withdraw(memberId, reason);
        return ResponseEntity.noContent().build();
    }
}
