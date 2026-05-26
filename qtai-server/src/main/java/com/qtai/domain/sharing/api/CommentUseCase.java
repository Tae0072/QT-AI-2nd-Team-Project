package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.CommentResponse;
import com.qtai.domain.sharing.api.dto.CommentCreateRequest;

/**
 * 댓글 작성 · 삭제 UseCase 포트.
 *
 * POST   /api/v1/sharing-posts/{postId}/comments   — 댓글 작성
 * DELETE /api/v1/comments/{commentId}              — 댓글 삭제 (본인만)
 *
 * SharingPost.commentsEnabled=false 이면 작성 불가 (403).
 */
public interface CommentUseCase {

    // TODO: CommentResponse create(Long memberId, Long postId, CommentCreateRequest request);
    // TODO: void delete(Long memberId, Long commentId);
}
