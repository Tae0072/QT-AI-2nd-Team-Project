package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 묵상 달력 streak 계산 단위 테스트(P1-9, 2026-06-05 Lead 결정).
 *
 * <p>회귀 방지: (1) 오늘 미저장이면 어제부터 anchor, (2) 월 경계와 무관하게 윈도로 연속 카운트,
 * (3) 중간 공백에서 streak이 끊긴다.
 */
@ExtendWith(MockitoExtension.class)
class MeditationCalendarServiceTest {

    // 오늘 = 2026-06-10 (Asia/Seoul). UTC 03:00 → KST 12:00 → 06-10.
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-10T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private NoteRepository noteRepository;

    private MeditationCalendarService service() {
        return new MeditationCalendarService(noteRepository, CLOCK);
    }

    private long streakFor(LocalDate... savedDates) {
        when(noteRepository.findSavedCalendarNotes(eq(1L), any(), any())).thenReturn(List.of());
        List<LocalDateTime> timestamps = java.util.Arrays.stream(savedDates)
                .map(d -> d.atTime(9, 0)).toList();
        when(noteRepository.findSavedMeditationTimestamps(eq(1L), any(), any())).thenReturn(timestamps);

        MeditationCalendarResponse response = service().getCalendar(1L, YearMonth.of(2026, 6));
        return response.summary().meditationStreakDays();
    }

    @Test
    void 오늘까지_3일_연속이면_streak_3() {
        assertThat(streakFor(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 9), LocalDate.of(2026, 6, 8)))
                .isEqualTo(3);
    }

    @Test
    void 오늘_미저장이고_어제까지_연속이면_어제부터_카운트() {
        assertThat(streakFor(LocalDate.of(2026, 6, 9), LocalDate.of(2026, 6, 8)))
                .isEqualTo(2);
    }

    @Test
    void 중간에_공백이_있으면_streak이_끊긴다() {
        // 06-10 저장, 06-09 공백, 06-08 저장 → 오늘부터 1일에서 끊김
        assertThat(streakFor(LocalDate.of(2026, 6, 10), LocalDate.of(2026, 6, 8)))
                .isEqualTo(1);
    }

    @Test
    void 저장_이력이_없으면_streak_0() {
        assertThat(streakFor()).isZero();
    }
}
