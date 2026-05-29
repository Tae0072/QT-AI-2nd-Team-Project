package com.qtai.domain.mission.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * MissionProgressCalculator 단위 테스트 (회원 단위 계산).
 *
 * <p>검증: 신규 생성·지표 매핑·진행률, 목표 달성(completedAt)+100% 상한, 기존 갱신,
 * MONTHLY 외 건너뜀, target&lt;=0 미완료.
 */
class MissionProgressCalculatorTest {

    // 2026-05-29 12:00 KST → YearMonth 2026-05
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-29T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    private MemberMissionProgressRepository progressRepository;
    private GetMeditationCalendarUseCase getMeditationCalendarUseCase;
    private MissionProgressCalculator calculator;

    @BeforeEach
    void setUp() {
        progressRepository = Mockito.mock(MemberMissionProgressRepository.class);
        getMeditationCalendarUseCase = Mockito.mock(GetMeditationCalendarUseCase.class);
        calculator = new MissionProgressCalculator(
                progressRepository, getMeditationCalendarUseCase, FIXED_CLOCK);
    }

    private void stubCalendar(Long memberId, long savedDays, long savedNoteCount, long streak) {
        MeditationCalendarResponse resp = new MeditationCalendarResponse(
                "2026-05", List.of(),
                new MeditationCalendarResponse.Summary(savedDays, savedNoteCount, streak));
        when(getMeditationCalendarUseCase.getCalendar(eq(memberId), any(YearMonth.class))).thenReturn(resp);
    }

    @Test
    void 신규_진행률_생성_지표매핑_및_진행률() {
        stubCalendar(1L, 10, 25, 5);
        MissionDefinition def = definition(100L, MissionMetricType.MEDITATION_SAVED_DAYS,
                MissionPeriodType.MONTHLY, 20);
        when(progressRepository.findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(
                1L, 100L, LocalDate.of(2026, 5, 1))).thenReturn(Optional.empty());

        calculator.recalculateForMember(1L, List.of(def));

        ArgumentCaptor<MemberMissionProgress> captor = ArgumentCaptor.forClass(MemberMissionProgress.class);
        verify(progressRepository).save(captor.capture());
        MemberMissionProgress saved = captor.getValue();
        assertThat(saved.getCurrentCount()).isEqualTo(10);          // savedDays
        assertThat(saved.getTargetCountSnapshot()).isEqualTo(20);
        assertThat(saved.getProgressRate()).isEqualByComparingTo("50.00");
        assertThat(saved.isCompleted()).isFalse();
        assertThat(saved.getPeriodStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(saved.getPeriodEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    void 목표달성_completedAt_설정_및_진행률_100상한() {
        stubCalendar(1L, 0, 25, 0);
        MissionDefinition def = definition(100L, MissionMetricType.NOTE_SAVED_COUNT,
                MissionPeriodType.MONTHLY, 20); // 25 >= 20 → 달성, 125% → 100 상한
        when(progressRepository.findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(
                1L, 100L, LocalDate.of(2026, 5, 1))).thenReturn(Optional.empty());

        calculator.recalculateForMember(1L, List.of(def));

        ArgumentCaptor<MemberMissionProgress> captor = ArgumentCaptor.forClass(MemberMissionProgress.class);
        verify(progressRepository).save(captor.capture());
        assertThat(captor.getValue().getProgressRate()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().isCompleted()).isTrue();
    }

    @Test
    void 기존_진행률_갱신_save_미호출() {
        stubCalendar(1L, 0, 0, 6);
        MissionDefinition def = definition(100L, MissionMetricType.STREAK_DAYS,
                MissionPeriodType.MONTHLY, 7);
        MemberMissionProgress existing = MemberMissionProgress.builder()
                .memberId(1L).missionDefinitionId(100L)
                .periodStartDate(LocalDate.of(2026, 5, 1)).periodEndDate(LocalDate.of(2026, 5, 31))
                .currentCount(3).targetCountSnapshot(7).progressRate(new BigDecimal("42.86"))
                .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0)).build();
        when(progressRepository.findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(
                1L, 100L, LocalDate.of(2026, 5, 1))).thenReturn(Optional.of(existing));

        calculator.recalculateForMember(1L, List.of(def));

        verify(progressRepository, never()).save(any());
        assertThat(existing.getCurrentCount()).isEqualTo(6);
        assertThat(existing.getProgressRate()).isEqualByComparingTo("85.71"); // 6/7*100
        assertThat(existing.getLastCalculatedAt()).isNotNull();
    }

    @Test
    void MONTHLY_외_주기는_건너뜀() {
        stubCalendar(1L, 10, 10, 10);
        MissionDefinition daily = definition(200L, MissionMetricType.MEDITATION_SAVED_DAYS,
                MissionPeriodType.DAILY, 1);

        calculator.recalculateForMember(1L, List.of(daily));

        verify(progressRepository, never())
                .findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(any(), any(), any());
        verify(progressRepository, never()).save(any());
    }

    @Test
    void target_0이하면_미완료_진행률_0() {
        stubCalendar(1L, 5, 5, 5);
        MissionDefinition def = definition(100L, MissionMetricType.MEDITATION_SAVED_DAYS,
                MissionPeriodType.MONTHLY, 0); // target 0 — 방어적 처리
        when(progressRepository.findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(
                1L, 100L, LocalDate.of(2026, 5, 1))).thenReturn(Optional.empty());

        calculator.recalculateForMember(1L, List.of(def));

        ArgumentCaptor<MemberMissionProgress> captor = ArgumentCaptor.forClass(MemberMissionProgress.class);
        verify(progressRepository).save(captor.capture());
        assertThat(captor.getValue().getProgressRate()).isEqualByComparingTo("0.00");
        assertThat(captor.getValue().isCompleted()).isFalse();
    }

    private MissionDefinition definition(Long id, MissionMetricType metric,
                                         MissionPeriodType period, int target) {
        MissionDefinition def = MissionDefinition.builder()
                .code("CODE_" + id).title("미션 " + id)
                .metricType(metric).periodType(period).targetCount(target)
                .status(MissionDefinitionStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0)).build();
        setId(def, id);
        return def;
    }

    private void setId(Object entity, Long id) {
        try {
            Field f = entity.getClass().getDeclaredField("id");
            f.setAccessible(true);
            f.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
