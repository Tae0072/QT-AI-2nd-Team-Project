package com.qtai.domain.sharing.api.dto;

import java.time.LocalDateTime;

/** 관리자 회원 상세 — 회원이 공유한 나눔글 1건 요약. */
public record AdminMemberPostItem(
        Long id,
        String status,
        String title,
        String category,
        LocalDateTime createdAt
) {
}
