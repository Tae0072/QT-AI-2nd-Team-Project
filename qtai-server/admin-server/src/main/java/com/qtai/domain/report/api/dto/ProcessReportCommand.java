package com.qtai.domain.report.api.dto;

/**
 * 관리자 신고 처리 명령.
 *
 * @param adminId        처리 관리자 ID
 * @param reportId       대상 신고 ID
 * @param action         후속 조치(예: HIDE_TARGET / NONE) — 본 구현은 기록만, 실제 대상 숨김은 후속
 * @param reason         처리 사유
 * @param notifyReporter 신고자 알림 여부 — 본 구현은 플래그만 보존, 실제 발송은 후속
 */
public record ProcessReportCommand(
        Long adminId,
        Long reportId,
        String action,
        String reason,
        boolean notifyReporter) {
}
