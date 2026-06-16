package com.qtai.domain.note.api.dto;

import java.time.LocalDateTime;

/**
 * 관리자 회원 상세 — 노트 1건의 전체 내용(운영 조회).
 *
 * <p>본문(body)과 묵상 4섹션(R/I/A/P)을 포함한다. 운영(신고 대응 등) 목적의 읽기 전용 상세다.
 */
public record AdminNoteDetail(
        Long id,
        Long qtPassageId,
        String category,
        String status,
        String visibility,
        String title,
        String body,
        String rememberSection,
        String interpretSection,
        String applySection,
        String praySection,
        LocalDateTime createdAt
) {
}
