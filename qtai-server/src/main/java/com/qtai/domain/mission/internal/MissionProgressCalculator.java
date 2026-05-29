package com.qtai.domain.mission.internal;

import com.qtai.domain.mission.api.RecalculateMissionProgressUseCase;
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
 * 미션 진행률 계산 서비스 (쓰기 경로).
 *
 * <p>note 도메인의 월간 묵상 집계({@link GetMeditationCalendarUseCase})를 소비해
 * 회원별 MONTHLY 미션 진행률을 계산·upsert한다(ERD §2.24, 진행률 = LEAST(current/target*100, 100)).
 *
 * <p>제약: note가 월 단위 집계만 제공하므로 현재는 MONTHLY 주기 미션만 계산한다. DAILY/WEEKLY는
 * note 기간별 집계 api가 생긴 뒤 후속 지원한다.
 *
 * <p>도메인 경계: 다른 도메인은 note의 api/UseCase로만 호출하고 Long FK만 보관한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class MissionProgressCalculator implements RecalculateMissionProgressUseCase {

    private final MemberMissionProgressRepository progressRepository;
    private final MissionDefinitionRepository definitionRepository;
    private final GetMeditationCalendarUseCase getMeditationCalendarUseCase;
    private final Clock clock;

    @Override
    public void recalculate(Long memberId) {
        YearMonth month = YearMonth.now(clock);
        LocalDate periodStart = month.atDay(1);
        LocalDate periodEnd = month.atEndOfMonth();
        LocalDateTime now = LocalDateTime.now(clock);

        MeditationCalendarResponse.Summary summary =
                getMeditationCalendarUseCase.getCalendar(memberId, month).summary();

        List<MissionDefinition> activeDefinitions = definitionRepository.findByStatus(MissionDefinitionStatus.ACTIVE);
        for (MissionDefinition def : activeDefinitions) {
            if (def.getPeriodType() != MissionPeriodType.MONTHLY) {
                // note가 월간 집계만 제공 — DAILY/WEEKLY는 후속 지원.
                continue;
            }
            int current = metricValue(def.getMetricType(), summary);
            upsert(memberId, def, periodStart, periodEnd, current, now);
        }
    }

    @Override
    public void recalculateAllEnrolled() {
        List<Long> memberIds = progressRepository.findDistinctMemberIds();
        log.info("미션 진행률 배치 시작: 대상 회원 {}명", memberIds.size());
        for (Long memberId : memberIds) {
            try {
                recalculate(memberId);
            } catch (Exception e) {
                // 한 회원 실패가 배치 전체를 멈추지 않도록 격리 — 실패 로그 후 다음 회원 진행.
                log.warn("미션 진행률 재계산 실패: memberId={}", memberId, e);
            }
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
        boolean reached = current >= target;

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

    /** 진행률 = LEAST(current / target * 100, 100), 소수 둘째 자리 반올림. */
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
