package com.qtai.domain.sharing.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.DeleteSharingPostUseCase;
import com.qtai.domain.sharing.api.GetSharingPostUseCase;
import com.qtai.domain.sharing.api.ListMySharingPostsUseCase;
import com.qtai.domain.sharing.api.ListSharingPostsUseCase;
import com.qtai.domain.sharing.api.PublishNoteUseCase;
import com.qtai.domain.sharing.api.SharingPostVisibilityUseCase;
import com.qtai.domain.sharing.api.ToggleLikeUseCase;
import com.qtai.domain.sharing.api.dto.LikeResponse;
import com.qtai.domain.sharing.api.dto.MySharingPostListResponse;
import com.qtai.domain.sharing.api.dto.PublishNoteRequest;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import com.qtai.domain.sharing.api.dto.SharingPostResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 나눔 피드 REST 엔드포인트. base path: /api/v1/sharing-posts
 *
 * 토큰 기반 공유({@code /api/v1/shares}, SharingController)와는 별개의 커뮤니티 피드다.
 */
@RestController
@RequiredArgsConstructor
public class SharingPostController {

    private final ListSharingPostsUseCase listSharingPostsUseCase;
    private final ListMySharingPostsUseCase listMySharingPostsUseCase;
    private final GetSharingPostUseCase getSharingPostUseCase;
    private final PublishNoteUseCase publishNoteUseCase;
    private final ToggleLikeUseCase toggleLikeUseCase;
    private final DeleteSharingPostUseCase deleteSharingPostUseCase;
    private final SharingPostVisibilityUseCase sharingPostVisibilityUseCase;

    @GetMapping("/api/v1/sharing-posts")
    public ApiResponse<SharingPostListResponse> list(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(listSharingPostsUseCase.list(authenticatedMemberId, category, q, pageable));
    }

    /** GET /api/v1/me/sharing-posts — 내가 쓴 나눔 글 목록(공개+숨김, 04 §4.4.5, 화면 M-05). */
    @GetMapping("/api/v1/me/sharing-posts")
    public ApiResponse<MySharingPostListResponse> listMine(
            @AuthenticationPrincipal Long memberId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(listMySharingPostsUseCase.listMine(authenticatedMemberId, status, pageable));
    }

    @GetMapping("/api/v1/sharing-posts/{postId}")
    public ApiResponse<SharingPostResponse> get(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(getSharingPostUseCase.getDetail(authenticatedMemberId, postId));
    }

    /** POST /api/v1/notes/{noteId}/share — 노트를 나눔 피드에 공유. */
    @PostMapping("/api/v1/notes/{noteId}/share")
    public ResponseEntity<ApiResponse<SharingPostResponse>> publish(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("noteId") Long noteId,
            @Valid @RequestBody PublishNoteRequest request) {
        Long authenticatedMemberId = requireMemberId(memberId);
        SharingPostResponse response = publishNoteUseCase.publish(authenticatedMemberId, noteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @PostMapping("/api/v1/sharing-posts/{postId}/like")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LikeResponse> like(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(toggleLikeUseCase.like(authenticatedMemberId, postId));
    }

    @DeleteMapping("/api/v1/sharing-posts/{postId}/like")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlike(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        toggleLikeUseCase.unlike(authenticatedMemberId, postId);
    }

    // TODO: 관리자(ADMIN+OPERATOR) 강제 삭제·hide는 04 §4.4.6 — v1 범위 밖. 이후 admin 도메인에서 확장.

    /** DELETE /api/v1/sharing-posts/{postId} — 작성자 본인이 나눔 글을 삭제(soft delete). */
    @DeleteMapping("/api/v1/sharing-posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        deleteSharingPostUseCase.delete(authenticatedMemberId, postId);
    }

    /** PATCH /api/v1/sharing-posts/{postId}/hide — 작성자 본인이 나눔 글을 공개 중단(숨김). */
    @PatchMapping("/api/v1/sharing-posts/{postId}/hide")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void hide(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        sharingPostVisibilityUseCase.hide(authenticatedMemberId, postId);
    }

    /** PATCH /api/v1/sharing-posts/{postId}/show — 작성자 본인이 숨긴 글을 되돌리기(공개). */
    @PatchMapping("/api/v1/sharing-posts/{postId}/show")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void show(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        sharingPostVisibilityUseCase.show(authenticatedMemberId, postId);
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
