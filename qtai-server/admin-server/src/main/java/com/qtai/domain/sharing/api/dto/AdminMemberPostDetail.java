package com.qtai.domain.sharing.api.dto;

import java.time.LocalDateTime;

/**
 * 관리자 회원 상세 — 공유한 나눔글 1건의 전체 내용(운영 조회).
 *
 * <p>공개 시점 스냅샷(제목·본문·분류·구절 라벨)을 담는다. 운영(신고 대응 등) 목적의 읽기 전용 상세다.
 */
public record AdminMemberPostDetail(
        Long id,
        String status,
        String title,
        String body,
        String category,
        String verseLabel,
        Long noteId,
        int likeCount,
        int commentCount,
        LocalDateTime createdAt
) {
}
