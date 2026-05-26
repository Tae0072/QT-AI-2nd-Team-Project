package com.qtai.domain.sharing.api.dto;

/**
 * 나눔 게시글 응답 DTO.
 *
 * GET /api/v1/sharing-posts, GET /api/v1/sharing-posts/{postId} 에서 사용.
 */
public record SharingPostResponse(
    // TODO: Long id;
    // TODO: String nicknameSnapshot;       — 공개 시점 닉네임
    // TODO: String contentSnapshot;        — 노트 본문 스냅샷
    // TODO: int likeCount;
    // TODO: int commentCount;
    // TODO: boolean commentsEnabled;
    // TODO: String status;                 — PUBLISHED / DELETED
    // TODO: String createdAt;
) {}
