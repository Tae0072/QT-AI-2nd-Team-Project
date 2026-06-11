package com.qtai.domain.qt.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import com.qtai.domain.qt.client.sum.SuTodayBibleClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 00:02 성서유니온 오늘 QT 수집 스케줄러 토글 검증.
 *
 * <p>일일 배치는 도메인 서비스(service-bible)가 소유한다(코드리뷰 2026-06-10 결정 A).
 * admin-server 복사본은 {@code qt.today-source.sum.enabled=false}가 기본값이며,
 * 토글이 꺼져 있으면 외부(성서유니온) 호출·저장·백필을 일절 수행하지 않아야 한다.
 */
class SuTodayPassageImportSchedulerToggleTest {

    private final SuTodayBibleClient client = mock(SuTodayBibleClient.class);
    private final QtTodayPassageImportService importService = mock(QtTodayPassageImportService.class);
    private final QtPassageRepository qtPassageRepository = mock(QtPassageRepository.class);
    private final Clock clock =
            Clock.fixed(Instant.parse("2026-06-11T00:02:00Z"), ZoneOffset.UTC);

    private SuTodayPassageImportScheduler disabledScheduler() {
        return new SuTodayPassageImportScheduler(
                client, importService, qtPassageRepository, clock, false);
    }

    @Test
    @DisplayName("토글 off면 기동 보강(importTodayOnStartup)은 아무것도 호출하지 않는다")
    void importTodayOnStartup_disabled_doesNothing() {
        disabledScheduler().importTodayOnStartup();

        verifyNoInteractions(client, importService, qtPassageRepository);
    }

    @Test
    @DisplayName("토글 off면 00:02 정기 수집(importToday)은 아무것도 호출하지 않는다")
    void importToday_disabled_doesNothing() {
        disabledScheduler().importToday();

        verifyNoInteractions(client, importService, qtPassageRepository);
    }
}
