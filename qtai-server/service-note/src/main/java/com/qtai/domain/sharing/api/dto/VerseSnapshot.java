package com.qtai.domain.sharing.api.dto;

/**
 * 나눔 글의 본문 범위 스냅샷 (04 §4.4.1 verseSnapshot).
 *
 * @param rangeLabel 사람이 읽는 본문 범위 라벨 (예: "창세기 1:1-5"). 없으면 null.
 */
public record VerseSnapshot(
        String rangeLabel
) {}
