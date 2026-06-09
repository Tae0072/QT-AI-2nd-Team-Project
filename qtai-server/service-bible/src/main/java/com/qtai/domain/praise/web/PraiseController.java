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
 *   <li>/api/v1/admin/praise-songs — 큐레이션 곡 등록 (콘텐츠 서비스에서는 미제공)</li>
 *   <li>/api/v1/me/praise-songs — 내 찬양 저장·삭제</li>
 * </ul>
 *
 * <p>목록 응답은 Spring Data {@code Page}를 그대로 노출하지 않고 표준
 * {@link PageResponse}로 매핑한다(PR #2 이월: Page 내부 구조의 계약 누출 방지).
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
     * POST /api/v1/admin/praise-songs — 큐레이션 곡 등록 (콘텐츠 서비스에서는 미제공).
     *
     * <p>관리자 등록 기능(admin_users.admin_role=CONTENT_CREATOR 이중검증 포함)은 admin-server가
     * 담당한다(2026-06-09 분리 설계). 이 콘텐츠 서비스에서는 도달 불가해야 하므로, SecurityConfig의
     * {@code /api/v1/admin/**} denyAll에 더해 메서드 보안에서도 {@code denyAll()}로 이중 차단한다.
     * {@code hasRole('ADMIN')} 단독 허용은 admin_role 검증 없이 열리는 우회 위험이 있어 사용하지 않는다
     * (CLAUDE.md §5, 리뷰 §4).
     */
    @PreAuthorize("denyAll()")
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
