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
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

/**
 * MissionProgressCalculator 단위 테스트.
 *
 * <p>검증: 신규 진행 생성, 기존 진행 갱신, 목표 달성(completedAt), 진행률 100% 상한,
 * MONTHLY 외 주기 건너뜀, 지표별 매핑, 배치 회원 반복.
 */
class MissionProgressCalculatorTest {

    // 2026-05-29 12:00 KST → YearMonth 2026-05
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-05-29T03:00:00Z"), ZoneId.of("Asia/Seoul"));

    private MemberMissionProgressRepository progressRepository;
    private MissionDefinitionRepository definitionRepository;
    private GetMeditationCalendarUseCase getMeditationCalendarUseCase;
    private MissionProgressCalculator calculator;

    @BeforeEach
    void setUp() {
        progressRepository = Mockito.mock(MemberMissionProgressRepository.class);
        definitionRepository = Mockito.mock(MissionDefinitionRepository.class);
        getMeditationCalendarUseCase = Mockito.mock(GetMeditationCalendarUseCase.class);
        calculator = new MissionProgressCalculator(
                progressRepository, definitionRepository, getMeditationCalendarUseCase, FIXED_CLOCK);
    }

    private void stubCalendar(Long memberId, long savedDays, long savedNoteCount, long streak) {
        MeditationCalendarResponse resp = new MeditationCalendarResponse(
                "2026-05", List.of(),
                new MeditationCalendarResponse.Summary(savedDays, savedNoteCount, streak));
        when(getMeditationCalendarUseCase.getCalendar(eq(memberId), any(YearMonth.class))).thenReturn(resp);
    }

    @Test
    void recalculate_신규_진행률_생성_지표매핑_및_진행률() {
        stubCalendar(1L, 10, 25, 5);
        MissionDefinition def = definition(100L, MissionMetricType.MEDITATION_SAVED_DAYS,
                MissionPeriodType.MONTHLY, 20);
        when(definitionRepository.findByStatus(MissionDefinitionStatus.ACTIVE)).thenReturn(List.of(def));
        when(progressRepository.findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(
                1L, 100L, LocalDate.of(2026, 5, 1))).thenReturn(Optional.empty());

        calculator.recalculate(1L);

        ArgumentCaptor<MemberMissionProgress> captor = ArgumentCaptor.forClass(MemberMissionProgress.class);
        verify(progressRepository).save(captor.capture());
        MemberMissionProgress saved = captor.getValue();
        assertThat(saved.getMissionDefinitionId()).isEqualTo(100L);
        assertThat(saved.getCurrentCount()).isEqualTo(10);          // savedDays
        assertThat(saved.getTargetCountSnapshot()).isEqualTo(20);
        assertThat(saved.getProgressRate()).isEqualByComparingTo("50.00");
        assertThat(saved.isCompleted()).isFalse();
        assertThat(saved.getPeriodStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(saved.getPeriodEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    void recalculate_목표달성_completedAt_설정_및_진행률_100상한() {
        stubCalendar(1L, 0, 25, 0);
        MissionDefinition def = definition(100L, MissionMetricType.NOTE_SAVED_COUNT,
                MissionPeriodType.MONTHLY, 20); // 25 >= 20 → 달성, 125% → 100 상한
        when(definitionRepository.findByStatus(MissionDefinitionStatus.ACTIVE)).thenReturn(List.of(def));
        when(progressRepository.findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(
                1L, 100L, LocalDate.of(2026, 5, 1))).thenReturn(Optional.empty());

        calculator.recalculate(1L);

        ArgumentCaptor<MemberMissionProgress> captor = ArgumentCaptor.forClass(MemberMissionProgress.class);
        verify(progressRepository).save(captor.capture());
        MemberMissionProgress saved = captor.getValue();
        assertThat(saved.getCurrentCount()).isEqualTo(25);          // savedNoteCount
        assertThat(saved.getProgressRate()).isEqualByComparingTo("100.00");
        assertThat(saved.isCompleted()).isTrue();
    }

    @Test
    void recalculate_기존_진행률_업데이트_save_미호출() {
        stubCalendar(1L, 0, 0, 6);
        MissionDefinition def = definition(100L, MissionMetricType.STREAK_DAYS,
                MissionPeriodType.MONTHLY, 7);
        when(definitionRepository.findByStatus(MissionDefinitionStatus.ACTIVE)).thenReturn(List.of(def));
        MemberMissionProgress existing = MemberMissionProgress.builder()
                .memberId(1L).missionDefinitionId(100L)
                .periodStartDate(LocalDate.of(2026, 5, 1)).periodEndDate(LocalDate.of(2026, 5, 31))
                .currentCount(3).targetCountSnapshot(7).progressRate(new BigDecimal("42.86"))
                .createdAt(java.time.LocalDateTime.of(2026, 5, 1, 0, 0)).build();
        when(progressRepository.findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(
                1L, 100L, LocalDate.of(2026, 5, 1))).thenReturn(Optional.of(existing));

        calculator.recalculate(1L);

        // 관리 엔티티 dirty checking으로 갱신 → save 미호출, 필드만 변경
        verify(progressRepository, never()).save(any());
        assertThat(existing.getCurrentCount()).isEqualTo(6);        // meditationStreakDays
        assertThat(existing.getProgressRate()).isEqualByComparingTo("85.71"); // 6/7*100
        assertThat(existing.getLastCalculatedAt()).isNotNull();
    }

    @Test
    void recalculate_MONTHLY_외_주기는_건너뜀() {
        stubCalendar(1L, 10, 10, 10);
        MissionDefinition daily = definition(200L, MissionMetricType.MEDITATION_SAVED_DAYS,
                MissionPeriodType.DAILY, 1);
        when(definitionRepository.findByStatus(MissionDefinitionStatus.ACTIVE)).thenReturn(List.of(daily));

        calculator.recalculate(1L);

        verify(progressRepository, never())
                .findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(any(), any(), any());
        verify(progressRepository, never()).save(any());
    }

    @Test
    void recalculateAllEnrolled_각_회원_재계산_한명_실패해도_계속() {
        when(progressRepository.findDistinctMemberIds()).thenReturn(List.of(1L, 2L));
        when(definitionRepository.findByStatus(MissionDefinitionStatus.ACTIVE)).thenReturn(List.of());
        stubCalendar(1L, 1, 1, 1);
        // 회원 2는 집계 조회 실패 → 격리되어 배치는 계속
        when(getMeditationCalendarUseCase.getCalendar(eq(2L), any(YearMonth.class)))
                .thenThrow(new RuntimeException("note down"));

        calculator.recalculateAllEnrolled();

        verify(getMeditationCalendarUseCase).getCalendar(eq(1L), any(YearMonth.class));
        verify(getMeditationCalendarUseCase).getCalendar(eq(2L), any(YearMonth.class));
    }

    private MissionDefinition definition(Long id, MissionMetricType metric,
                                         MissionPeriodType period, int target) {
        MissionDefinition def = MissionDefinition.builder()
                .code("CODE_" + id).title("미션 " + id)
                .metricType(metric).periodType(period).targetCount(target)
                .status(MissionDefinitionStatus.ACTIVE)
                .createdAt(java.time.LocalDateTime.of(2026, 5, 1, 0, 0)).build();
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
