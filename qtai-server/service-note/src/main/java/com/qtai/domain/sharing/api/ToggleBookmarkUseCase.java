package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.BookmarkResponse;

/**
 * 나눔 게시글 저장(북마크) · 해제 UseCase 포트.
 *
 * POST   /api/v1/sharing-posts/{postId}/bookmark   — 저장
 * DELETE /api/v1/sharing-posts/{postId}/bookmark   — 저장 해제
 *
 * 정책:
 * - 대상은 PUBLISHED 게시글만 (없으면 404)
 * - 저장 추가는 멱등 — 이미 저장돼 있으면 중복 INSERT 없이 bookmarked=true로 끝낸다
 *   ((sharingPostId, memberId) UNIQUE가 최종 backstop)
 * - 저장 해제는 멱등 — 저장한 적 없어도 204로 끝낸다
 */
public interface ToggleBookmarkUseCase {

    BookmarkResponse bookmark(Long memberId, Long postId);

    void unbookmark(Long memberId, Long postId);
}
