package com.qtai.domain.sharing.api.dto;

import java.util.List;

/**
 * 나눔 상세의 본문 범위 스냅샷 (04 §4.4.2 verseSnapshot).
 *
 * 목록(§4.4.1)의 {@link VerseSnapshot}은 rangeLabel만 갖지만, 상세는 개별 절 배열까지 포함한다.
 *
 * @param rangeLabel 본문 범위 라벨 (예: "창세기 1:1-5")
 * @param verses     개별 절 목록. 다중 절 스냅샷 저장은 v2 작업이라 현재는 빈 배열
 */
public record VerseSnapshotDetail(
        String rangeLabel,
        List<VerseLine> verses
) {}
