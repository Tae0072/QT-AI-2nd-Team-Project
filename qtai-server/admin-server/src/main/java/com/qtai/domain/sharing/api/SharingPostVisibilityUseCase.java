package com.qtai.domain.sharing.api;

/**
 * 나눔 게시글 공개 중단(숨김) · 되돌리기 UseCase 포트(F-10, 04 §4.4.6).
 *
 * PATCH /api/v1/sharing-posts/{postId}/hide — 공개 중단 (PUBLISHED → HIDDEN)
 * PATCH /api/v1/sharing-posts/{postId}/show — 되돌리기   (HIDDEN → PUBLISHED)
 *
 * 정책:
 * - 작성자 본인만 가능 (아니면 403 FORBIDDEN), 없는 글은 404 SHARING_POST_NOT_FOUND
 * - 멱등 — 이미 목표 상태면 조용히 끝낸다(204)
 * - 삭제된(DELETED) 글은 숨김·되돌리기 불가 → 409 INVALID_STATUS_TRANSITION
 * - 관리자(ADMIN+OPERATOR) 강제 숨김은 v1 범위 밖 — admin 도메인에서 이후 구현
 */
public interface SharingPostVisibilityUseCase {

    void hide(Long memberId, Long postId);

    void show(Long memberId, Long postId);
}
