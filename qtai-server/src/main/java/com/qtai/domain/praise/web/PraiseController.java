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
 * API 명세서 §4.6.4 기준.
 * - /api/v1/praise-songs    → 큐레이션 목록 (전체 회원)
 * - /api/v1/me/praise-songs → 내 찬양 저장/삭제
 */
@RestController
@RequiredArgsConstructor
public class PraiseController {

    private final CreatePraiseUseCase createPraiseUseCase;
    private final ListPraiseUseCase listPraiseUseCase;
    private final SaveMemberPraiseSongUseCase saveMemberPraiseSongUseCase;
    private final ListMemberPraiseSongUseCase listMemberPraiseSongUseCase;

    // ── 큐레이션 곡 (전체 회원 열람 가능) ──

    /** GET /api/v1/praise-songs?status=ACTIVE — 큐레이션 곡 목록. */
    @GetMapping("/api/v1/praise-songs")
    public ResponseEntity<ApiResponse<Page<PraiseResponse>>> listPraiseSongs(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<PraiseResponse> page = listPraiseUseCase.listActive(pageable);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    /** POST /api/v1/praise-songs — 큐레이션 곡 등록 (ADMIN only). */
    @PostMapping("/api/v1/praise-songs")
    public ResponseEntity<ApiResponse<PraiseResponse>> createPraiseSong(
            @Valid @RequestBody PraiseCreateRequest request) {
        // @PreAuthorize는 SecurityConfig에서 처리 예정 (auth-jwt 브랜치)
        PraiseResponse response = createPraiseUseCase.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    // ── 내 찬양 저장 목록 ──

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
            @PathVariable Long id) {
        saveMemberPraiseSongUseCase.remove(memberId, id);
        return ResponseEntity.noContent().build();
    }
}
