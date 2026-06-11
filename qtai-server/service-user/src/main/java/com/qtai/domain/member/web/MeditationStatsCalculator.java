package com.qtai.domain.member.web;

import com.qtai.domain.member.api.dto.DashboardResponse.StatsWidget;
import com.qtai.domain.member.api.dto.DashboardResponse.StatsWidget.WeekMonth;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse.Day;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

/**
 * 마이페이지 "나의 묵상" 통계 계산기 — service-note 묵상 달력 응답을 주간/월간/연속 위젯 값으로 환산한다.
 *
 * <p>집계 의미(2026-06-11 피드백 반영):
 * <ul>
 *   <li>하루 인정: 카테고리 무관, SAVED 노트가 1건이라도 있으면 그 날을 묵상일 1일로 센다
 *       (service-note 달력의 {@code Day.saved}와 동일 기준 — 임시저장(DRAFT)·삭제 노트 제외).</li>
 *   <li>이번 주: 오늘이 속한 ISO 주(월요일 시작)의 주 시작일~오늘까지의 묵상일/노트 수.
 *       주가 이전 달에 걸치면 이전 달 달력을 함께 받아 합산한다.</li>
 *   <li>이번 달: 이번 달 1일~말일 집계(달력 summary 그대로).</li>
 *   <li>연속: service-note가 계산한 streak(오늘 저장 시 오늘부터, 미저장 시 어제까지 인정)를 그대로 쓴다.</li>
 *   <li>반영 시점: 저장 즉시(savedAt 기준, 매 조회 시 재계산) — 익일 반영 아님.</li>
 * </ul>
 *
 * <p>순수 계산만 담당한다(Clock·HTTP 없음) — 단위 테스트로 주 경계/월 경계를 고정 검증하기 위함.
 * web 레이어의 응답 가공 헬퍼이므로 컨트롤러와 같은 패키지에 둔다(ArchUnit: web→internal 금지).
 */
final class MeditationStatsCalculator {

    private MeditationStatsCalculator() {
    }

    /** 오늘이 속한 주(월요일 시작)가 이전 달로 거슬러 올라가는지 — 이전 달 달력 추가 조회 필요 여부. */
    static boolean weekCrossesPreviousMonth(LocalDate today) {
        return today.with(DayOfWeek.MONDAY).isBefore(today.withDayOfMonth(1));
    }

    /**
     * 통계 위젯 값을 만든다.
     *
     * @param today         오늘(KST 기준 호출자가 Clock으로 환산)
     * @param currentMonth  이번 달 묵상 달력(필수)
     * @param previousMonth 이전 달 묵상 달력 — 주가 이전 달에 걸칠 때만 전달, 아니면 null
     */
    static StatsWidget build(LocalDate today,
                             MeditationCalendarResponse currentMonth,
                             MeditationCalendarResponse previousMonth) {
        Map<LocalDate, Day> byDate = new HashMap<>();
        indexDays(byDate, previousMonth);
        indexDays(byDate, currentMonth);

        LocalDate weekStart = today.with(DayOfWeek.MONDAY);
        int weekMeditationDays = 0;
        int weekSavedNoteCount = 0;
        for (LocalDate date = weekStart; !date.isAfter(today); date = date.plusDays(1)) {
            Day day = byDate.get(date);
            if (day == null) {
                continue;
            }
            if (day.saved()) {
                weekMeditationDays++;
            }
            weekSavedNoteCount += (int) day.savedNoteCount();
        }

        var monthSummary = currentMonth.summary();
        return new StatsWidget(
                new WeekMonth(weekSavedNoteCount, weekMeditationDays),
                new WeekMonth((int) monthSummary.savedNoteCount(), (int) monthSummary.savedDays()),
                (int) monthSummary.meditationStreakDays()
        );
    }

    private static void indexDays(Map<LocalDate, Day> byDate, MeditationCalendarResponse calendar) {
        if (calendar == null || calendar.days() == null) {
            return;
        }
        for (Day day : calendar.days()) {
            byDate.put(day.date(), day);
        }
    }
}
