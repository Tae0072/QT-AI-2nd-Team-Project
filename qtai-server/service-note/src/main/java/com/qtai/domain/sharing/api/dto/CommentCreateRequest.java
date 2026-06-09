package com.qtai.domain.sharing.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 댓글 작성 요청 DTO.
 *
 * POST /api/v1/sharing-posts/{postId}/comments
 *
 * - body: 댓글 본문. 빈 값이면 400(@NotBlank), 1000자 초과면 400(@Size).
 */
public record CommentCreateRequest(
        @NotBlank @Size(max = 1000) String body
) {}
