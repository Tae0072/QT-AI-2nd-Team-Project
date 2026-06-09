package com.qtai.domain.praise.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.domain.praise.api.CreatePraiseUseCase;
import com.qtai.domain.praise.api.ListMemberPraiseSongUseCase;
import com.qtai.domain.praise.api.ListPraiseUseCase;
import com.qtai.domain.praise.api.SaveMemberPraiseSongUseCase;
import com.qtai.domain.praise.api.dto.MemberPraiseSongCreateRequest;
import com.qtai.domain.praise.api.dto.MemberPraiseSongResponse;
import com.qtai.domain.praise.api.dto.PraiseCreateRequest;
import com.qtai.domain.praise.api.dto.PraiseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 찬양 REST 엔드포인트.
 *
 * <p>API 명세서 §4.6.4 기준.
 * <ul>
 *   <li>/api/v1/praise-songs — 큐레이션 목록 (전체 회원)</li>
 *   <li>/api/v1/admin/praise-songs — 큐레이션 곡 등록 (ADMIN, SecurityConfig 에서 역할 검증)</li>
 *   <li>/api/v1/me/praise-songs — 내 찬양 저장·삭제</li>
 * </ul>
 *
 * <p>관리자 권한 정책: /api/v1/admin/** 는 SecurityConfig 에서 hasRole('ADMIN') 검증.
 * admin_role 세부 권한은 admin 도메인 구현 후 서비스 레이어에서 추가 검증 예정.
 */
@RestController
@RequiredArgsConstructor
public class PraiseController {

    private final CreatePraiseUseCase createPraiseUseCase;
    private final ListPraiseUseCase listPraiseUseCase;
    private final SaveMemberPraiseSongUseCase saveMemberPraiseSongUseCase;
    private final ListMemberPraiseSongUseCase listMemberPraiseSongUseCase;

    // ── 큐레이션 곡 (전체 회원 읽기 가능) ──

    /** GET /api/v1/praise-songs — 큐레이션 곡 목록. */
    @GetMapping("/api/v1/praise-songs")
    public ResponseEntity<ApiResponse<Page<PraiseResponse>>> listPraiseSongs(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PraiseResponse> page = listPraiseUseCase.listActive(pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * POST /api/v1/admin/praise-songs — 큐레이션 곡 등록 (ADMIN only).
     *
     * <p>SecurityConfig 에서 /api/v1/admin/** → hasRole('ADMIN') 검증.
     * <p>TODO: admin 도메인 구현 후 admin_users.admin_role 세부 권한(CONTENT_CREATOR) 추가 검증 필요
     *   — CLAUDE.md §5 "OPERATOR, REVIEWER, CONTENT_CREATOR, SUPER_ADMIN 중 API 명세에 맞는 세부 권한을 요구"
     */
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/api/v1/admin/praise-songs")
    public ResponseEntity<ApiResponse<PraiseResponse>> createPraiseSong(
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestBody PraiseCreateRequest request) {
        PraiseResponse response = createPraiseUseCase.create(adminId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── 내 찬양 저장·목록·삭제 ──

    /** GET /api/v1/me/praise-songs — 내 찬양 목록. */
    @GetMapping("/api/v1/me/praise-songs")
    public ResponseEntity<ApiResponse<Page<MemberPraiseSongResponse>>> listMyPraiseSongs(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<MemberPraiseSongResponse> page = listMemberPraiseSongUseCase.listMy(memberId, pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /** POST /api/v1/me/praise-songs — 내 찬양에 곡 저장. */
    @PostMapping("/api/v1/me/praise-songs")
    public ResponseEntity<ApiResponse<MemberPraiseSongResponse>> savePraiseSong(
            @AuthenticationPrincipal Long memberId,
            @Valid @RequestBody MemberPraiseSongCreateRequest request) {
        MemberPraiseSongResponse response = saveMemberPraiseSongUseCase.save(memberId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    /** DELETE /api/v1/me/praise-songs/{id} — 내 찬양에서 곡 제거. */
    @DeleteMapping("/api/v1/me/praise-songs/{id}")
    public ResponseEntity<Void> removePraiseSong(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("id") Long id) {
        saveMemberPraiseSongUseCase.remove(memberId, id);
        return ResponseEntity.noContent().build();
    }
}
