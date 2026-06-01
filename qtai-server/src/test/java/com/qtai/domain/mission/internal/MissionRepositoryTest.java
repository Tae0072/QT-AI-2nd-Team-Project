package com.qtai.domain.mission.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.qtai.config.JpaAuditingConfig;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * mission 도메인 리포지토리 통합 테스트.
 *
 * <p>H2 create-drop 으로 신규 2테이블의 복합 UK·파생 쿼리·DDL(precision 등)을 검증한다.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
@ActiveProfiles("test")
class MissionRepositoryTest {

    @Autowired
    private MissionDefinitionRepository definitionRepository;

    @Autowired
    private MemberMissionProgressRepository progressRepository;

    @Autowired
    private TestEntityManager em;

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 29, 12, 0);

    // ── mission_definitions ──

    @Test
    @DisplayName("UK uk_mission_definitions_code — 동일 code 중복 삽입 시 예외")
    void definition_code_unique() {
        em.persistAndFlush(definition("MEDITATION_30"));

        assertThrows(Exception.class, () -> em.persistAndFlush(definition("MEDITATION_30")));
    }

    @Test
    @DisplayName("findByIdIn — 주어진 ID 집합만 조회")
    void definition_findByIdIn() {
        MissionDefinition a = em.persistAndFlush(definition("A"));
        MissionDefinition b = em.persistAndFlush(definition("B"));
        em.persistAndFlush(definition("C"));

        List<MissionDefinition> found = definitionRepository.findByIdIn(List.of(a.getId(), b.getId()));

        assertThat(found).extracting(MissionDefinition::getCode)
                .containsExactlyInAnyOrder("A", "B");
    }

    // ── member_mission_progress ──

    @Test
    @DisplayName("UK uk_member_mission_period — (member, definition, period_start) 중복 시 예외")
    void progress_period_unique() {
        em.persistAndFlush(progress(1L, 100L, LocalDate.of(2026, 5, 1), new BigDecimal("10.00")));

        assertThrows(Exception.class, () ->
                em.persistAndFlush(progress(1L, 100L, LocalDate.of(2026, 5, 1), new BigDecimal("20.00"))));
    }

    @Test
    @DisplayName("UK uk_member_mission_period — 다른 기간이면 삽입 가능")
    void progress_different_period_ok() {
        em.persistAndFlush(progress(1L, 100L, LocalDate.of(2026, 5, 1), new BigDecimal("10.00")));

        MemberMissionProgress next = progress(1L, 100L, LocalDate.of(2026, 6, 1), new BigDecimal("0.00"));
        em.persistAndFlush(next);

        assertThat(next.getId()).isNotNull();
    }

    @Test
    @DisplayName("findByMemberIdOrderByPeriodStartDateDesc — 본인 진행률만 집계 시작일 내림차순")
    void progress_findByMember_orderDesc() {
        em.persistAndFlush(progress(1L, 100L, LocalDate.of(2026, 4, 1), new BigDecimal("100.00")));
        em.persistAndFlush(progress(1L, 100L, LocalDate.of(2026, 5, 1), new BigDecimal("50.00")));
        em.persistAndFlush(progress(2L, 100L, LocalDate.of(2026, 5, 1), new BigDecimal("30.00")));

        List<MemberMissionProgress> result =
                progressRepository.findByMemberIdOrderByPeriodStartDateDesc(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getPeriodStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(result.get(1).getPeriodStartDate()).isEqualTo(LocalDate.of(2026, 4, 1));
    }

    @Test
    @DisplayName("progress_rate DECIMAL(5,2) — 소수 둘째 자리까지 보존")
    void progress_rate_precision() {
        MemberMissionProgress saved = em.persistAndFlush(
                progress(1L, 100L, LocalDate.of(2026, 5, 1), new BigDecimal("33.33")));
        em.clear();

        MemberMissionProgress reloaded = progressRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getProgressRate()).isEqualByComparingTo("33.33");
    }

    // ── helpers ──

    private MissionDefinition definition(String code) {
        return MissionDefinition.builder()
                .code(code)
                .title(code + " 미션")
                .metricType(MissionMetricType.MEDITATION_SAVED_DAYS)
                .periodType(MissionPeriodType.MONTHLY)
                .targetCount(30)
                .status(MissionDefinitionStatus.ACTIVE)
                .createdAt(NOW)
                .build();
    }

    private MemberMissionProgress progress(Long memberId, Long definitionId,
                                           LocalDate periodStart, BigDecimal rate) {
        return MemberMissionProgress.builder()
                .memberId(memberId)
                .missionDefinitionId(definitionId)
                .periodStartDate(periodStart)
                .periodEndDate(periodStart.plusMonths(1).minusDays(1))
                .currentCount(0)
                .targetCountSnapshot(30)
                .progressRate(rate)
                .createdAt(NOW)
                .build();
    }
}
