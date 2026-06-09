package com.qtai.domain.qt.internal;

import java.time.LocalDate;
import java.util.List;

/**
 * 앱이 읽는 일자별 QT 스냅샷 페이로드(member-agnostic).
 *
 * <p>사용자별 값(draftNoteId)·런타임 캐시 상태(cacheStatus)는 담지 않는다 — 공유 정적 파일이므로
 * 특정 사용자 정보가 섞이면 안 된다. 본문 콘텐츠 컨텍스트(날짜·passage·절 ID·공개여부)만 담는다.
 * (검증용 주석 원문·참조 자료 등 금지 데이터는 포함하지 않는다. CLAUDE.md §7·§8)
 */
public record QtDailySnapshot(
        LocalDate date,
        Long qtPassageId,
        String title,
        List<Long> verseIds,
        boolean published,
        String generatedAt
) {
}
