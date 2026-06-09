package com.qtai.domain.praise.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.dto.PageResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
 *   <li>/api/v1/admin/praise-songs — 큐레이션 곡 등록 (관리자 전용)</li>
 *   <li>/api/v1/me/praise-songs — 내 찬양 저장·삭제</li>
 * </ul>
 *
 * <p>목록 응답은 Spring Data {@code Page}를 그대로 노출하지 않고 표준
 * {@link PageResponse}로 매핑한다(PR #2 이월: Page 내부 구조의 계약 누출 방지).
 *
 * <p>관리자 경로(/api/v1/admin/**)는 SecurityConfig에서 denyAll로 차단된다. 이 콘텐츠
 * 서비스는 관리자 기능을 제공하지 않으며, admin_role 세부 권한 검증은 admin-server가 담당한다
 * (2026-06-09 분리 설계). 아래 등록 엔드포인트는 계약 보존용이며 런타임에는 도달 불가하다.
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
    public ResponseEntity<ApiResponse<PageResponse<PraiseResponse>>> listPraiseSongs(
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<PraiseResponse> page = PageResponse.from(listPraiseUseCase.listActive(pageable));
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /**
     * POST /api/v1/admin/praise-songs — 큐레이션 곡 등록 (관리자 전용, denyAll로 차단).
     *
     * <p>SecurityConfig에서 /api/v1/admin/** → denyAll. admin_users.admin_role 세부 권한
     * (CONTENT_CREATOR)을 포함한 관리자 등록 기능은 admin-server에서 제공한다.
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
    public ResponseEntity<ApiResponse<PageResponse<MemberPraiseSongResponse>>> listMyPraiseSongs(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 20) Pageable pageable) {
        PageResponse<MemberPraiseSongResponse> page =
                PageResponse.from(listMemberPraiseSongUseCase.listMy(memberId, pageable));
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
