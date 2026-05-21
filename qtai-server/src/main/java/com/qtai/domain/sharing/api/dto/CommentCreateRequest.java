package com.qtai.domain.sharing.api.dto;

/**
 * 댓글 작성 요청 DTO.
 *
 * POST /api/v1/sharing-posts/{postId}/comments
 */
public record CommentCreateRequest(
    // TODO: @NotBlank String content;
) {}
