package com.qtai.domain.mission.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.qtai.domain.mission.api.dto.MissionProgressResponse;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * MissionService(읽기 모델) 단위 테스트.
 *
 * <p>검증 범위: 진행률 → 정의 매핑, 완료/미완료 판정, 빈 결과 처리, 정의 누락 방어.
 */
class MissionServiceTest {

    private MemberMissionProgressRepository progressRepository;
    private MissionDefinitionRepository definitionRepository;
    private MissionService missionService;

    @BeforeEach
    void setUp() {
        progressRepository = Mockito.mock(MemberMissionProgressRepository.class);
        definitionRepository = Mockito.mock(MissionDefinitionRepository.class);
        missionService = new MissionService(progressRepository, definitionRepository);
    }

    @Test
    void getMissionProgress_진행률없으면_빈리스트_정의조회_생략() {
        when(progressRepository.findByMemberIdOrderByPeriodStartDateDesc(1L))
                .thenReturn(List.of());

        List<MissionProgressResponse> result = missionService.getMissionProgress(1L);

        assertThat(result).isEmpty();
        verify(definitionRepository, never()).findByIdIn(anyList());
    }

    @Test
    void getMissionProgress_정의와_매핑하여_반환() {
        MissionDefinition def = definition(5L, "MEDITATION_30", "한 달 묵상 30일",
                MissionMetricType.MEDITATION_SAVED_DAYS, MissionPeriodType.MONTHLY, 30);
        MemberMissionProgress progress = MemberMissionProgress.builder()
                .memberId(1L)
                .missionDefinitionId(5L)
                .periodStartDate(LocalDate.of(2026, 5, 1))
                .periodEndDate(LocalDate.of(2026, 5, 31))
                .currentCount(10)
                .targetCountSnapshot(30)
                .progressRate(new BigDecimal("33.33"))
                .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0))
                .build();
        when(progressRepository.findByMemberIdOrderByPeriodStartDateDesc(1L))
                .thenReturn(List.of(progress));
        when(definitionRepository.findByIdIn(List.of(5L)))
                .thenReturn(List.of(def));

        List<MissionProgressResponse> result = missionService.getMissionProgress(1L);

        assertThat(result).hasSize(1);
        MissionProgressResponse r = result.get(0);
        assertThat(r.missionDefinitionId()).isEqualTo(5L);
        assertThat(r.code()).isEqualTo("MEDITATION_30");
        assertThat(r.title()).isEqualTo("한 달 묵상 30일");
        assertThat(r.metricType()).isEqualTo("MEDITATION_SAVED_DAYS");
        assertThat(r.periodType()).isEqualTo("MONTHLY");
        assertThat(r.currentCount()).isEqualTo(10);
        assertThat(r.targetCount()).isEqualTo(30);
        assertThat(r.progressRate()).isEqualByComparingTo("33.33");
        assertThat(r.completed()).isFalse();
        assertThat(r.completedAt()).isNull();
    }

    @Test
    void getMissionProgress_완료된_미션은_completed_true() {
        MissionDefinition def = definition(7L, "NOTE_10", "노트 10개",
                MissionMetricType.NOTE_SAVED_COUNT, MissionPeriodType.WEEKLY, 10);
        MemberMissionProgress completed = MemberMissionProgress.builder()
                .memberId(1L)
                .missionDefinitionId(7L)
                .periodStartDate(LocalDate.of(2026, 5, 18))
                .periodEndDate(LocalDate.of(2026, 5, 24))
                .currentCount(10)
                .targetCountSnapshot(10)
                .progressRate(new BigDecimal("100.00"))
                .completedAt(LocalDateTime.of(2026, 5, 23, 9, 0))
                .createdAt(LocalDateTime.of(2026, 5, 18, 0, 0))
                .build();
        when(progressRepository.findByMemberIdOrderByPeriodStartDateDesc(1L))
                .thenReturn(List.of(completed));
        when(definitionRepository.findByIdIn(List.of(7L)))
                .thenReturn(List.of(def));

        List<MissionProgressResponse> result = missionService.getMissionProgress(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).completed()).isTrue();
        assertThat(result.get(0).completedAt()).isEqualTo(LocalDateTime.of(2026, 5, 23, 9, 0));
    }

    @Test
    void getMissionProgress_정의_누락시_코드_제목_null_진행값은_유지() {
        MemberMissionProgress orphan = MemberMissionProgress.builder()
                .memberId(1L)
                .missionDefinitionId(99L)
                .periodStartDate(LocalDate.of(2026, 5, 1))
                .periodEndDate(LocalDate.of(2026, 5, 31))
                .currentCount(3)
                .targetCountSnapshot(30)
                .progressRate(new BigDecimal("10.00"))
                .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0))
                .build();
        when(progressRepository.findByMemberIdOrderByPeriodStartDateDesc(1L))
                .thenReturn(List.of(orphan));
        when(definitionRepository.findByIdIn(List.of(99L)))
                .thenReturn(List.of());

        List<MissionProgressResponse> result = missionService.getMissionProgress(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isNull();
        assertThat(result.get(0).title()).isNull();
        assertThat(result.get(0).currentCount()).isEqualTo(3);
        assertThat(result.get(0).progressRate()).isEqualByComparingTo("10.00");
    }

    @Test
    void getMissionProgress_HIDDEN_정의_진행률은_대시보드에서_제외() {
        MissionDefinition active = definition(5L, "ACTIVE_M", "활성 미션",
                MissionMetricType.NOTE_SAVED_COUNT, MissionPeriodType.MONTHLY, 10);
        MissionDefinition hidden = MissionDefinition.builder()
                .code("HIDDEN_M").title("숨김 미션")
                .metricType(MissionMetricType.NOTE_SAVED_COUNT).periodType(MissionPeriodType.MONTHLY)
                .targetCount(10).status(MissionDefinitionStatus.HIDDEN)
                .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0))
                .build();
        setId(hidden, 6L);

        MemberMissionProgress activeProgress = MemberMissionProgress.builder()
                .memberId(1L).missionDefinitionId(5L)
                .periodStartDate(LocalDate.of(2026, 5, 1)).periodEndDate(LocalDate.of(2026, 5, 31))
                .currentCount(5).targetCountSnapshot(10).progressRate(new BigDecimal("50.00"))
                .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0)).build();
        MemberMissionProgress hiddenProgress = MemberMissionProgress.builder()
                .memberId(1L).missionDefinitionId(6L)
                .periodStartDate(LocalDate.of(2026, 5, 1)).periodEndDate(LocalDate.of(2026, 5, 31))
                .currentCount(8).targetCountSnapshot(10).progressRate(new BigDecimal("80.00"))
                .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0)).build();

        when(progressRepository.findByMemberIdOrderByPeriodStartDateDesc(1L))
                .thenReturn(List.of(activeProgress, hiddenProgress));
        when(definitionRepository.findByIdIn(anyList()))
                .thenReturn(List.of(active, hidden));

        List<MissionProgressResponse> result = missionService.getMissionProgress(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).code()).isEqualTo("ACTIVE_M");
    }

    private MissionDefinition definition(Long id, String code, String title,
                                         MissionMetricType metricType,
                                         MissionPeriodType periodType, int target) {
        MissionDefinition def = MissionDefinition.builder()
                .code(code)
                .title(title)
                .metricType(metricType)
                .periodType(periodType)
                .targetCount(target)
                .status(MissionDefinitionStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0))
                .build();
        setId(def, id);
        return def;
    }

    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
