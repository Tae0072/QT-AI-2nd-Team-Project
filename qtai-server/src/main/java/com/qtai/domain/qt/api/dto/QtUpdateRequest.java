package com.qtai.domain.qt.api.dto;

/**
 * QT 수정 요청 DTO.
 *
 * null 필드는 "변경 없음"으로 해석 (PATCH 시맨틱).
 */
public record QtUpdateRequest(
        // TODO: String title            — null이면 유지
        // TODO: String content          — null이면 유지
        // TODO: Long bibleVerseId       — null이면 유지 (제거하려면 별도 필드/플래그 필요)
        // TODO: QtVisibility visibility — null이면 유지
) {}
