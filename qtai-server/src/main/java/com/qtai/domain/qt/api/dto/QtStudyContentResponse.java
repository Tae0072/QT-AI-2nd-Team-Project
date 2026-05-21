package com.qtai.domain.qt.api.dto;

/**
 * QT 학습 콘텐츠(해설·용어·요약) 응답 DTO.
 *
 * GET /api/v1/qt/{qtPassageId}/study-content
 *
 * 검증용 주석·참조 자료는 포함하지 않는다 (CLAUDE.md §7).
 */
public record QtStudyContentResponse(
    // TODO: Long qtPassageId;
    // TODO: List<ExplanationItem> explanations;   — verse_explanations ACTIVE 기준
    // TODO: List<TermItem> terms;                 — 단어·용어 설명
    // TODO: String summary;                       — 본문 요약
) {}
