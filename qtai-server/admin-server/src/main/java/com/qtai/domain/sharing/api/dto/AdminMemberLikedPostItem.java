package com.qtai.domain.sharing.api.dto;

import java.time.LocalDateTime;

/** 관리자 회원 상세 — 회원이 좋아요한 나눔글 1건 요약. */
public record AdminMemberLikedPostItem(
        Long postId,
        String title,
        String status,
        LocalDateTime likedAt
) {
}
