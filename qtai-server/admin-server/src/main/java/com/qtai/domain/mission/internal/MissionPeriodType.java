package com.qtai.domain.mission.internal;

/**
 * 미션 집계 주기.
 *
 * <p>ERD: mission_definitions.period_type (VARCHAR(20), 기본값 'MONTHLY').
 */
public enum MissionPeriodType {
    DAILY,
    WEEKLY,
    MONTHLY
}
