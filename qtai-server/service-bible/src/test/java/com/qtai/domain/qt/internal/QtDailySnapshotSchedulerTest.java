package com.qtai.domain.qt.internal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * {@link QtDailySnapshotScheduler} 단위 테스트 — 스케줄 트리거가 KST 기준 '오늘' 날짜로 익스포트를 호출한다.
 */
class QtDailySnapshotSchedulerTest {

    @Test
    @DisplayName("exportTodaySnapshot은 KST 오늘 날짜로 스냅샷 서비스를 호출한다")
    void triggers_export_for_today_in_kst() {
        // 2026-06-08T15:30Z == 2026-06-09 00:30 KST → KST 날짜는 06-09.
        Clock clock = Clock.fixed(Instant.parse("2026-06-08T15:30:00Z"), ZoneId.of("Asia/Seoul"));
        TodayQtSnapshotService snapshotService = mock(TodayQtSnapshotService.class);
        when(snapshotService.exportSnapshot(LocalDate.of(2026, 6, 9)))
                .thenReturn(Optional.of("/snap/2026-06-09.json"));

        new QtDailySnapshotScheduler(snapshotService, clock).exportTodaySnapshot();

        verify(snapshotService).exportSnapshot(LocalDate.of(2026, 6, 9));
    }
}
