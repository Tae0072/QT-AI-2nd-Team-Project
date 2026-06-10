package com.qtai.domain.study.api.dto;

/**
 * Today QT 응답용 학습 콘텐츠 가용성.
 *
 * @param simulatorStatus 시뮬레이터 상태 — READY(승인 클립 존재) / MISSING(없음).
 *                        FAILED(검증 실패)·DISABLED(운영 비활성) 판정은 후속 과제.
 * @param hasExplanation  승인(APPROVED·ACTIVE) 해설 존재 여부 — 해설 진입점 활성 기준
 */
public record QtStudyAvailability(
        String simulatorStatus,
        boolean hasExplanation
) {
}
