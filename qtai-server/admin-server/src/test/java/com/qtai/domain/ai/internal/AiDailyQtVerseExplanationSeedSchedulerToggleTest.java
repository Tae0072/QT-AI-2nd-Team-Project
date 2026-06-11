package com.qtai.domain.ai.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 00:05 AI 해설 시딩 스케줄러 토글 검증.
 *
 * <p>일일 배치는 도메인 서비스(service-ai)가 소유한다(코드리뷰 2026-06-10 결정 A).
 * admin-server 복사본은 {@code ai.daily-qt-verse-seed.enabled=false}가 기본값이며,
 * 토글이 꺼져 있으면 시딩 서비스·배치 기록을 일절 호출하지 않아야 한다.
 */
class AiDailyQtVerseExplanationSeedSchedulerToggleTest {

    private final AiDailyQtVerseExplanationSeedService service =
            mock(AiDailyQtVerseExplanationSeedService.class);
    private final AiBatchMonitoringService monitoringService =
            mock(AiBatchMonitoringService.class);
    private final Clock clock =
            Clock.fixed(Instant.parse("2026-06-11T00:05:00Z"), ZoneOffset.UTC);

    @Test
    @DisplayName("토글 off면 seedDaily는 시딩 서비스와 배치 기록을 호출하지 않는다")
    void seedDaily_disabled_doesNothing() {
        AiDailyQtVerseExplanationSeedScheduler scheduler =
                new AiDailyQtVerseExplanationSeedScheduler(service, monitoringService, false, clock);

        scheduler.seedDaily();

        verifyNoInteractions(service, monitoringService);
    }
}
