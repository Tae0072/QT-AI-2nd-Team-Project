package com.qtai.domain.mission.internal;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * MissionProgressCoordinator 단위 테스트 — 회원별 위임/격리.
 *
 * <p>코디네이터는 비트랜잭션이며 회원마다 {@link MissionProgressCalculator}(별도 빈, @Transactional)를
 * 호출한다. 한 회원 실패가 다음 회원으로 전파되지 않는지(루프 계속), ACTIVE 정의를 1회만 로드하는지 검증한다.
 */
class MissionProgressCoordinatorTest {

    private MissionProgressCalculator calculator;
    private MissionDefinitionRepository definitionRepository;
    private MemberMissionProgressRepository progressRepository;
    private MissionProgressCoordinator coordinator;

    @BeforeEach
    void setUp() {
        calculator = Mockito.mock(MissionProgressCalculator.class);
        definitionRepository = Mockito.mock(MissionDefinitionRepository.class);
        progressRepository = Mockito.mock(MemberMissionProgressRepository.class);
        coordinator = new MissionProgressCoordinator(
                calculator, definitionRepository, progressRepository);
    }

    private MissionDefinition activeDef() {
        MissionDefinition def = MissionDefinition.builder()
                .code("M").title("미션").metricType(MissionMetricType.MEDITATION_SAVED_DAYS)
                .periodType(MissionPeriodType.MONTHLY).targetCount(20)
                .status(MissionDefinitionStatus.ACTIVE)
                .createdAt(LocalDateTime.of(2026, 5, 1, 0, 0)).build();
        return def;
    }

    @Test
    void recalculateAllEnrolled_한_회원_실패해도_나머지_계속_그리고_정의_1회_로드() {
        when(definitionRepository.findByStatus(MissionDefinitionStatus.ACTIVE))
                .thenReturn(List.of(activeDef()));
        when(progressRepository.findDistinctMemberIds()).thenReturn(List.of(1L, 2L, 3L));
        // 회원 2 실패 → 격리되어 1·3은 계속 처리되어야 한다
        doNothing().when(calculator).recalculateForMember(eq(1L), anyList());
        doThrow(new RuntimeException("note down")).when(calculator).recalculateForMember(eq(2L), anyList());
        doNothing().when(calculator).recalculateForMember(eq(3L), anyList());

        coordinator.recalculateAllEnrolled();

        verify(calculator).recalculateForMember(eq(1L), anyList());
        verify(calculator).recalculateForMember(eq(2L), anyList());
        verify(calculator).recalculateForMember(eq(3L), anyList());
        // ACTIVE 정의는 회원 수와 무관하게 1회만 로드
        verify(definitionRepository, times(1)).findByStatus(MissionDefinitionStatus.ACTIVE);
    }

    @Test
    void recalculate_단일회원_계산기에_위임() {
        when(definitionRepository.findByStatus(MissionDefinitionStatus.ACTIVE))
                .thenReturn(List.of(activeDef()));

        coordinator.recalculate(7L);

        verify(calculator).recalculateForMember(eq(7L), anyList());
    }
}
