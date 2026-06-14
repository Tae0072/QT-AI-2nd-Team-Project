package com.qtai.domain.sharing.web;

import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.sharing.api.ListBookmarksUseCase;
import com.qtai.domain.sharing.api.ToggleBookmarkUseCase;
import com.qtai.domain.sharing.api.dto.BookmarkResponse;
import com.qtai.domain.sharing.api.dto.SharingPostListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 나눔 게시글 저장(북마크) REST 엔드포인트.
 *
 * POST   /api/v1/sharing-posts/{postId}/bookmark   — 저장
 * DELETE /api/v1/sharing-posts/{postId}/bookmark   — 저장 해제
 * GET    /api/v1/me/bookmarks                       — 내 저장 목록(최근 저장순)
 *
 * 좋아요(SharingPostController)와 같은 인증·응답 규칙을 따른다.
 */
@RestController
@RequiredArgsConstructor
public class BookmarkController {

    private final ToggleBookmarkUseCase toggleBookmarkUseCase;
    private final ListBookmarksUseCase listBookmarksUseCase;

    @PostMapping("/api/v1/sharing-posts/{postId}/bookmark")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookmarkResponse> bookmark(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(toggleBookmarkUseCase.bookmark(authenticatedMemberId, postId));
    }

    @DeleteMapping("/api/v1/sharing-posts/{postId}/bookmark")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unbookmark(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("postId") Long postId) {
        Long authenticatedMemberId = requireMemberId(memberId);
        toggleBookmarkUseCase.unbookmark(authenticatedMemberId, postId);
    }

    /** GET /api/v1/me/bookmarks — 내가 저장한 나눔 글 목록(피드와 동일 카드, 최근 저장순). */
    @GetMapping("/api/v1/me/bookmarks")
    public ApiResponse<SharingPostListResponse> listBookmarks(
            @AuthenticationPrincipal Long memberId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long authenticatedMemberId = requireMemberId(memberId);
        return ApiResponse.success(listBookmarksUseCase.listBookmarks(authenticatedMemberId, pageable));
    }

    private Long requireMemberId(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return memberId;
    }
}
