package com.qtai.domain.sharing.api.dto;

import java.util.List;

/**
 * 댓글 목록 응답 (04 §4.4.4, 페이징).
 *
 * <p>{@link SharingPostListResponse}와 같은 페이징 필드 구조를 따른다.
 */
public record CommentListResponse(
        List<CommentResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {}
