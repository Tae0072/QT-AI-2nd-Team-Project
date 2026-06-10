package com.qtai.domain.qt.api.dto;

/**
 * 오늘 QT 통합 응답 DTO.
 *
 * GET /api/v1/qt/today
 *
 * "Today QT 100%": 본문·해설 진입점·노트 진입점·시뮬레이터 상태 포함.
 * simulatorStatus 는 READY·MISSING·FAILED·DISABLED 중 하나.
 * (CLAUDE.md §6 — 시뮬레이터 버튼은 READY 일 때만 활성화)
 *
 * cacheStatus 는 00:00~04:00 사전 생성 캐시 정책 노출용 (CLAUDE.md §6):
 *   HIT            — 캐시 적중 (정상)
 *   MISS           — 캐시 미스 (배치 미완료 상태, 클라이언트 재시도 권장)
 *   STALE_FALLBACK — 새 캐시 없음, 이전 캐시 제공 중 (00:00~04:00 구간)
 *   EMPTY          — 제공 가능한 데이터 없음
 *   DIRECT         — ID 기반 직접 조회(todayQt 캐시 미경유). GET /passages/{id} 응답용
 *
 * @param qtPassageId     QT 본문 식별자
 * @param passageDate     본문 날짜 (yyyy-MM-dd)
 * @param title           QT 제목
 * @param simulatorStatus 시뮬레이터 상태 (READY / MISSING / FAILED / DISABLED)
 * @param hasExplanation  해설 진입점 제공 여부 (승인된 해설 존재 시 true)
 * @param draftNoteId     사용자 DRAFT 노트 ID (없으면 null)
 * @param cacheStatus     캐시 상태 (HIT / MISS / STALE_FALLBACK / EMPTY)
 * @param range           Flutter 본문 조회용 권/장/절 범위
 */
public record TodayQtResponse(
        Long qtPassageId,
        String passageDate,
        String title,
        String simulatorStatus,
        boolean hasExplanation,
        Long draftNoteId,
        String cacheStatus,
        TodayQtRangeResponse range,
        String videoStatus
) {
    public TodayQtResponse(
            Long qtPassageId,
            String passageDate,
            String title,
            String simulatorStatus,
            boolean hasExplanation,
            Long draftNoteId,
            String cacheStatus,
            TodayQtRangeResponse range
    ) {
        this(qtPassageId, passageDate, title, simulatorStatus, hasExplanation, draftNoteId,
                cacheStatus, range, "MISSING");
    }

    public TodayQtResponse(
            Long qtPassageId,
            String passageDate,
            String title,
            String simulatorStatus,
            boolean hasExplanation,
            Long draftNoteId,
            String cacheStatus
    ) {
        this(qtPassageId, passageDate, title, simulatorStatus, hasExplanation, draftNoteId, cacheStatus, null);
    }
}
