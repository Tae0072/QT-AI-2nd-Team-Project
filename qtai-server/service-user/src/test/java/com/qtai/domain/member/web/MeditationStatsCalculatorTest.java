package com.qtai.domain.member.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.qtai.domain.member.api.dto.DashboardResponse.StatsWidget;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse.Day;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse.Summary;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@link MeditationStatsCalculator} 단위 테스트 — 주간(주 시작~오늘) 집계와 월 경계 주를 고정 날짜로 검증한다.
 *
 * <p>피드백 기준(2026-06-11): SAVED 노트가 1건이라도 있는 날 = 묵상 1일, 저장 즉시 반영.
 */
class MeditationStatsCalculatorTest {

    @Test
    @DisplayName("주중(수요일) 기준 — 이번 주는 월~오늘까지만 세고, 이번 달·streak은 summary를 그대로 쓴다")
    void build_주중() {
        // 2026-06-10(수). 이번 주(6/8 월~6/10 수) 중 6/8·6/10 저장, 지난주 6/5도 저장(주간 미포함).
        LocalDate today = LocalDate.of(2026, 6, 10);
        MeditationCalendarResponse june = calendar(
                YearMonth.of(2026, 6),
                Set.of(5, 8, 10),
                new Summary(3, 4, 2));

        StatsWidget stats = MeditationStatsCalculator.build(today, june, null);

        assertThat(stats.week().meditationDays()).isEqualTo(2);   // 6/8, 6/10
        assertThat(stats.week().savedNoteCount()).isEqualTo(2);
        assertThat(stats.month().meditationDays()).isEqualTo(3);  // summary.savedDays
        assertThat(stats.month().savedNoteCount()).isEqualTo(4);  // summary.savedNoteCount
        assertThat(stats.meditationStreakDays()).isEqualTo(2);    // summary 그대로
    }

    @Test
    @DisplayName("주가 이전 달에 걸치면 이전 달 달력의 저장일도 주간 집계에 합산한다")
    void build_월경계_주() {
        // 2026-07-01(수): 주 시작은 6/29(월) — 6/29·6/30은 이전 달 소속.
        LocalDate today = LocalDate.of(2026, 7, 1);
        MeditationCalendarResponse july = calendar(
                YearMonth.of(2026, 7),
                Set.of(1),
                new Summary(1, 1, 3));
        MeditationCalendarResponse june = calendar(
                YearMonth.of(2026, 6),
                Set.of(29, 30),
                new Summary(2, 2, 0));

        assertThat(MeditationStatsCalculator.weekCrossesPreviousMonth(today)).isTrue();
        StatsWidget stats = MeditationStatsCalculator.build(today, july, june);

        assertThat(stats.week().meditationDays()).isEqualTo(3);   // 6/29, 6/30, 7/1
        assertThat(stats.week().savedNoteCount()).isEqualTo(3);
        assertThat(stats.month().meditationDays()).isEqualTo(1);  // 7월 summary만
        assertThat(stats.meditationStreakDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("저장이 하나도 없으면 모두 0이다")
    void build_빈_달력() {
        LocalDate today = LocalDate.of(2026, 6, 10);
        MeditationCalendarResponse june = calendar(
                YearMonth.of(2026, 6),
                Set.of(),
                new Summary(0, 0, 0));

        StatsWidget stats = MeditationStatsCalculator.build(today, june, null);

        assertThat(stats.week().meditationDays()).isZero();
        assertThat(stats.week().savedNoteCount()).isZero();
        assertThat(stats.month().meditationDays()).isZero();
        assertThat(stats.meditationStreakDays()).isZero();
    }

    @Test
    @DisplayName("월요일이 오늘이면 주간 집계는 오늘 하루만 본다")
    void build_월요일() {
        // 2026-06-08(월): 주 시작=오늘. 전날(6/7 일) 저장은 주간에 포함되지 않는다.
        LocalDate today = LocalDate.of(2026, 6, 8);
        MeditationCalendarResponse june = calendar(
                YearMonth.of(2026, 6),
                Set.of(7, 8),
                new Summary(2, 2, 2));

        StatsWidget stats = MeditationStatsCalculator.build(today, june, null);

        assertThat(stats.week().meditationDays()).isEqualTo(1);   // 6/8만
    }

    // ── 헬퍼: 지정한 일자만 saved=true(노트 1건)인 한 달 달력을 만든다 ──
    private MeditationCalendarResponse calendar(YearMonth month, Set<Integer> savedDays, Summary summary) {
        List<Day> days = new ArrayList<>();
        for (int d = 1; d <= month.lengthOfMonth(); d++) {
            boolean saved = savedDays.contains(d);
            days.add(new Day(
                    month.atDay(d),
                    saved,
                    saved ? 1 : 0,
                    null,
                    saved ? List.of(NoteCategory.MEDITATION) : List.of()));
        }
        return new MeditationCalendarResponse(month.toString(), days, summary);
    }
}
