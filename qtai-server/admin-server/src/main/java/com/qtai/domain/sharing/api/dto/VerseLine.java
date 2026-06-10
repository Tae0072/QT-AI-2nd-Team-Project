package com.qtai.domain.sharing.api.dto;

/**
 * 나눔 상세의 개별 절 한 줄 (04 §4.4.2 verseSnapshot.verses[]).
 *
 * @param label      절 표기 (예: "창세기 1:1")
 * @param koreanText 절 본문(한글)
 */
public record VerseLine(
        String label,
        String koreanText
) {}
