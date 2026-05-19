package com.qtai.domain.bible.api.dto;

/** 성경 검색 요청 DTO. */
public record BibleSearchRequest(
        // TODO: String keyword       — 검색어 (필수, @NotBlank)
        // TODO: String translation   — 번역본 코드 (예: "NIV", "개역개정") — null 시 기본 번역
        // TODO: String book          — 특정 책으로 범위 제한 (null 시 전체)
        // TODO: Integer chapter      — 특정 장으로 범위 제한 (null 시 책 전체)
) {}
