package com.qtai.domain.mission.internal;

/**
 * 미션 정의 노출 상태.
 *
 * <p>ERD: mission_definitions.status (VARCHAR(20), 기본값 'ACTIVE').
 * <ul>
 *   <li>ACTIVE — 노출/집계 대상</li>
 *   <li>HIDDEN — 숨김</li>
 * </ul>
 */
public enum MissionDefinitionStatus {
    ACTIVE,
    HIDDEN
}
