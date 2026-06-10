package com.qtai.domain.sharing.api;

/**
 * 나눔 게시글 삭제 UseCase 포트(F-10, 04 §4.4.6).
 *
 * DELETE /api/v1/sharing-posts/{postId} — 작성자 본인이 자기 나눔 글을 삭제(soft delete).
 *
 * 정책:
 * - 작성자 본인만 삭제 가능 (아니면 403 FORBIDDEN), 없는 글은 404 SHARING_POST_NOT_FOUND
 * - soft delete — status=DELETED, deletedAt 기록 (행은 남긴다)
 * - 멱등 — 이미 삭제된 글이면 조용히 끝낸다(204)
 * - 관리자(ADMIN+OPERATOR) 강제 삭제는 v1 범위 밖 — admin 도메인에서 이후 구현
 */
public interface DeleteSharingPostUseCase {

    void delete(Long memberId, Long postId);
}
