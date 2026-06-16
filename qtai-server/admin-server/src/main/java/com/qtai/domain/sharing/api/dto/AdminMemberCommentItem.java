package com.qtai.domain.sharing.api.dto;

import java.time.LocalDateTime;

/** 관리자 회원 상세 — 회원이 작성한 댓글 1건 요약. */
public record AdminMemberCommentItem(
        Long id,
        Long sharingPostId,
        String body,
        boolean deleted,
        LocalDateTime createdAt
) {
}
