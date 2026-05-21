package com.qtai.domain.sharing.api;

/**
 * 나눔 게시글 좋아요 · 취소 UseCase 포트.
 *
 * POST   /api/v1/sharing-posts/{postId}/like   — 좋아요
 * DELETE /api/v1/sharing-posts/{postId}/like   — 좋아요 취소
 *
 * (sharingPostId, memberId) UNIQUE 제약으로 중복 방지.
 */
public interface ToggleLikeUseCase {

    // TODO: void like(Long memberId, Long postId);
    // TODO: void unlike(Long memberId, Long postId);
}
