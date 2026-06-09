package com.qtai.domain.mission.client.note;

import com.qtai.domain.note.api.GetMeditationCalendarUseCase;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import java.time.YearMonth;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * note 도메인 {@link GetMeditationCalendarUseCase} 임시 Mock (MSA 통합 전).
 *
 * <p>미션 진행률 계산({@code MissionProgressCalculator})이 월간 묵상 집계를 소비한다.
 * note 도메인은 service-note로 분리되어 실제 구현체가 없다. Day3 통합에서 RestClient
 * 어댑터로 교체하고 이 Mock은 삭제한다(CLAUDE.md §4).
 *
 * <p>안전 기본값: 빈 집계(savedDays=0)를 반환한다 → MONTHLY 미션 진행률 0%로 계산된다.
 */
@Slf4j
@Component
public class GetMeditationCalendarUseCaseMock implements GetMeditationCalendarUseCase {

    @Override
    public MeditationCalendarResponse getCalendar(Long memberId, YearMonth month) {
        log.warn("[MOCK] note.GetMeditationCalendarUseCase — 통합 전 임시 구현(빈 집계): memberId={}, month={}", memberId, month);
        return new MeditationCalendarResponse(
                month.toString(),
                List.of(),
                new MeditationCalendarResponse.Summary(0L, 0L, 0L));
    }
}
