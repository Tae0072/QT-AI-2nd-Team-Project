package com.qtai.domain.mission.internal;

import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 미션 진행률 계산 — <b>회원 단위 트랜잭션 경계</b>.
 *
 * <p>{@link MissionProgressCoordinator}가 회원마다 이 빈의 {@code recalculateForMember}를
 * (스프링 프록시 경유로) 호출해 <b>회원별 독립 트랜잭션</b>으로 처리한다. 따라서 한 회원의 실패(롤백)가
 * 같은 트랜잭션에 묶인 다른 회원에 전파되지 않는다. (자기호출로 트랜잭션이 합쳐지던 문제를 구조적으로 차단)
 *
 * <p>note 월간 묵상 집계({@link GetMeditationCalendarUseCase})를 소비해 MONTHLY 미션 진행률을
 * 계산·upsert한다(ERD §2.24, 진행률 = LEAST(current/target*100, 100)). note가 월간 집계만 제공하므로
 * MONTHLY 외 주기는 건너뛴다(후속 지원).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MissionProgressCalculator {

    private final MemberMissionProgressRepository progressRepository;
    private final GetMeditationCalendarUseCase getMeditationCalendarUseCase;
    private final Clock clock;

    /**
     * 한 회원의 ACTIVE MONTHLY 미션 진행률을 재계산·upsert한다. 독립 트랜잭션으로 실행된다.
     *
     * @param memberId          대상 회원
     * @param activeDefinitions ACTIVE 미션 정의(코디네이터가 1회 로드해 전달 — 회원마다 재조회하지 않음)
     */
    @Transactional
    public void recalculateForMember(Long memberId, List<MissionDefinition> activeDefinitions) {
        YearMonth month = YearMonth.now(clock);
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();
        LocalDateTime now = LocalDateTime.now(clock);

        MeditationCalendarResponse.Summary summary =
                getMeditationCalendarUseCase.getCalendar(memberId, month).summary();

        for (MissionDefinition def : activeDefinitions) {
            if (def.getPeriodType() != MissionPeriodType.MONTHLY) {
                continue; // note가 월간 집계만 제공 — DAILY/WEEKLY는 후속 지원
            }
            int current = metricValue(def.getMetricType(), summary);
            upsert(memberId, def, periodStart, periodEnd, current, now);
        }
    }

    private int metricValue(MissionMetricType metricType, MeditationCalendarResponse.Summary summary) {
        return (int) switch (metricType) {
            case MEDITATION_SAVED_DAYS -> summary.savedDays();
            case NOTE_SAVED_COUNT -> summary.savedNoteCount();
            case STREAK_DAYS -> summary.meditationStreakDays();
        };
    }

    private void upsert(Long memberId, MissionDefinition def, LocalDate periodStart,
                        LocalDate periodEnd, int current, LocalDateTime now) {
        int target = def.getTargetCount();
        BigDecimal rate = progressRate(current, target);
        boolean reached = target > 0 && current >= target;

        progressRepository
                .findByMemberIdAndMissionDefinitionIdAndPeriodStartDate(memberId, def.getId(), periodStart)
                .ifPresentOrElse(
                        existing -> existing.applyCalculation(current, rate, reached, now),
                        () -> progressRepository.save(MemberMissionProgress.builder()
                                .memberId(memberId)
                                .missionDefinitionId(def.getId())
                                .periodStartDate(periodStart)
                                .periodEndDate(periodEnd)
                                .currentCount(current)
                                .targetCountSnapshot(target)
                                .progressRate(rate)
                                .completedAt(reached ? now : null)
                                .lastCalculatedAt(now)
                                .createdAt(now)
                                .build()));
    }

    /** 진행률 = LEAST(current / target * 100, 100), 소수 둘째 자리 반올림. target<=0이면 0%. */
    private BigDecimal progressRate(int current, int target) {
        if (target <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal rate = BigDecimal.valueOf(current)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(target), 2, RoundingMode.HALF_UP);
        BigDecimal hundred = BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        return rate.min(hundred);
    }
}
