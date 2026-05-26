package com.qtai.domain.member.api.dto;

/**
 * 사용자 설정 응답 DTO.
 *
 * GET /api/v1/me/settings (§4.1.6)
 *
 * @param verseSelectionMode 절 선택 방식 — v1: SINGLE 만 지원, v2: RANGE 활성화 예정
 * @param writingMode        글쓰기 방식 — PLAIN / MARKDOWN
 */
public record MemberSettingsResponse(
        String verseSelectionMode,
        String writingMode
) {}
