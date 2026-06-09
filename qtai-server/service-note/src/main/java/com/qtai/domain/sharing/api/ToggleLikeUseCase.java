package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.LikeResponse;

/**
 * 나눔 게시글 좋아요 · 취소 UseCase 포트.
 *
 * POST   /api/v1/sharing-posts/{postId}/like   — 좋아요
 * DELETE /api/v1/sharing-posts/{postId}/like   — 좋아요 취소
 *
 * 정책:
 * - 대상은 PUBLISHED 게시글만 (없으면 404)
 * - like 중복은 409 DUPLICATE_LIKE ((sharingPostId, memberId) UNIQUE가 최종 backstop)
 * - unlike는 멱등 — 좋아요가 없어도 204로 끝낸다
 * - likeCount는 COUNT 재계산으로 실제 행 수에 맞춘다
 */
public interface ToggleLikeUseCase {

    LikeResponse like(Long memberId, Long postId);

    void unlike(Long memberId, Long postId);
}
