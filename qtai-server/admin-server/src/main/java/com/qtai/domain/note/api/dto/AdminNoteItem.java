package com.qtai.domain.note.api.dto;

import java.time.LocalDateTime;

/**
 * 관리자 회원 상세 — 회원이 작성한 노트 1건 요약(F-10 운영 조회).
 *
 * <p>본문 전체는 노출하지 않고 운영 식별에 필요한 메타데이터만 담는다.
 */
public record AdminNoteItem(
        Long id,
        Long qtPassageId,
        String category,
        String status,
        String visibility,
        String title,
        LocalDateTime createdAt
) {
}
