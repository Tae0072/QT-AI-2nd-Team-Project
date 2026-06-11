package com.qtai.domain.mission.internal;

/**
 * 미션 달성 지표 타입.
 *
 * <p>ERD: mission_definitions.metric_type (VARCHAR(30)).
 * <ul>
 *   <li>MEDITATION_SAVED_DAYS — 묵상 노트 저장 일수</li>
 *   <li>NOTE_SAVED_COUNT      — 저장 노트 개수</li>
 *   <li>STREAK_DAYS           — 연속 묵상 일수</li>
 * </ul>
 * <p>진행률 집계 기준은 ERD §2.24 "계산 기준" 참조(notes.status=SAVED 확정 노트만 집계).
 */
public enum MissionMetricType {
    MEDITATION_SAVED_DAYS,
    NOTE_SAVED_COUNT,
    STREAK_DAYS
}
