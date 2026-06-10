package com.qtai.domain.mission.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link MissionProgressCalculator} 단위 테스트 — MONTHLY 미션만 집계하고 진행률을 upsert하는지 검증.
 *
 * <p>시계는 매월 1일이 아닌 날(15일)로 고정해 "전월 마감 재계산" 분기를 타지 않게 한다.
 */
@ExtendWith(MockitoExtension.class)
class MissionProgressCalculatorTest {

    @Mock
    private MemberMissionProgressRepository progressRepository;
    @Mock
    private GetMeditationCalendarUseCase getMeditationCalendarUseCase;
    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-06-15T01:00:00Z"), ZoneId.of("Asia/Seoul"));

    @InjectMocks
    private MissionProgressCalculator calculator;

    private MissionDefinition definition(MissionPeriodType periodType) {
        return MissionDefinition.builder()
                .code("READ_DAYS")
                .title("이달의 묵상")
                .metricType(MissionMetricType.MEDITATION_SAVED_DAYS)
                .periodType(periodType)
                .targetCount(5)
                .status(MissionDefinitionStatus.ACTIVE)
                .createdAt(LocalDateTime.now(clock))
                .build();
    }

    private MeditationCalendarResponse calendarWithSavedDays(long savedDays) {
        return new MeditationCalendarResponse(
                "2026-06", List.of(),
                new MeditationCalendarResponse.Summary(savedDays, savedDays, savedDays));
    }

    @Test
    void MONTHLY미션은_진행률을_upsert한다() {
        when(getMeditationCalendarUseCase.getCalendar(eq(1L), any(YearMonth.class)))
                .thenReturn(calendarWithSavedDays(10));
        when(progressRepository.findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(any(), any(), any()))
                .thenReturn(Optional.empty());

        calculator.recalculateForMember(1L, List.of(definition(MissionPeriodType.MONTHLY)));

        verify(progressRepository).save(any(MemberMissionProgress.class));
    }

    @Test
    void MONTHLY가_아닌_미션은_건너뛴다() {
        when(getMeditationCalendarUseCase.getCalendar(eq(1L), any(YearMonth.class)))
                .thenReturn(calendarWithSavedDays(10));

        calculator.recalculateForMember(1L, List.of(definition(MissionPeriodType.DAILY)));

        verify(progressRepository, never()).save(any());
    }
}
