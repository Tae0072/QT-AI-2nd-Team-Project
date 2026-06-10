package com.qtai.domain.sharing.api.dto;

import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO (04 §4.4.4).
 *
 * @param nickname  작성자 공개 닉네임 (member 공개 프로필에서 조회)
 * @param ownedByMe 조회자가 이 댓글 작성자인지 (삭제 버튼 노출 판단)
 */
public record CommentResponse(
        Long id,
        Long sharingPostId,
        Long memberId,
        String nickname,
        String body,
        boolean ownedByMe,
        LocalDateTime createdAt
) {}
