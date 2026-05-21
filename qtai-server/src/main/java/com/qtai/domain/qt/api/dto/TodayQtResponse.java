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
 * @param qtPassageId     QT 본문 식별자
 * @param passageDate     본문 날짜 (yyyy-MM-dd)
 * @param title           QT 제목
 * @param simulatorStatus 시뮬레이터 상태 (READY / MISSING / FAILED / DISABLED)
 * @param hasExplanation  해설 진입점 제공 여부 (승인된 해설 존재 시 true)
 * @param draftNoteId     사용자 DRAFT 노트 ID (없으면 null)
 */
public record TodayQtResponse(
        Long qtPassageId,
        String passageDate,
        String title,
        String simulatorStatus,
        boolean hasExplanation,
        Long draftNoteId
) {}
