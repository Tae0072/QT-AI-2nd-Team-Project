package com.qtai.domain.sharing.api;

import com.qtai.domain.sharing.api.dto.CommentCreateRequest;
import com.qtai.domain.sharing.api.dto.CommentListResponse;
import com.qtai.domain.sharing.api.dto.CommentResponse;
import org.springframework.data.domain.Pageable;

/**
 * 댓글 작성 · 조회 · 삭제 UseCase 포트.
 *
 * POST   /api/v1/sharing-posts/{postId}/comments   — 댓글 작성
 * GET    /api/v1/sharing-posts/{postId}/comments   — 댓글 목록 (살아있는 것만, 시간순)
 * DELETE /api/v1/comments/{commentId}              — 댓글 삭제 (본인만, soft delete)
 *
 * 정책:
 * - 대상은 PUBLISHED 게시글만 (없으면 404)
 * - 댓글 OFF(commentsEnabled=false) 글에는 작성 불가 (409 INVALID_STATUS_TRANSITION)
 * - 삭제는 본인만 (403), 두 번 삭제해도 멱등
 * - commentCount는 COUNT 재계산으로 실제 행 수에 맞춘다
 */
public interface CommentUseCase {

    CommentResponse create(Long memberId, Long postId, CommentCreateRequest request);

    CommentListResponse list(Long memberId, Long postId, Pageable pageable);

    void delete(Long memberId, Long commentId);
}
