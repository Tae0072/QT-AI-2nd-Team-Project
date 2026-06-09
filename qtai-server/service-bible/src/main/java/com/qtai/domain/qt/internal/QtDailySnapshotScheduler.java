package com.qtai.domain.qt.internal;

import java.time.Clock;
import java.time.LocalDate;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 매일 00:00 KST에 오늘 QT 스냅샷을 생성·업로드하는 스케줄러.
 *
 * <p>{@code qt.snapshot.enabled=true}일 때만 빈이 생성된다(기본 false) — 테스트·기동 검증에서는
 * 스케줄러가 외부 스토리지를 건드리지 않는다. service-bible 앱에 {@code @EnableScheduling}이 이미
 * 켜져 있으므로 본 클래스는 활성화 게이트만 둔다. QT 공개 시각 정책(00:00 KST, CLAUDE.md §6)에 맞춘다.
 */
@Component
@ConditionalOnProperty(prefix = "qt.snapshot", name = "enabled", havingValue = "true")
public class QtDailySnapshotScheduler {

    private final TodayQtSnapshotService snapshotService;
    private final Clock clock;

    public QtDailySnapshotScheduler(TodayQtSnapshotService snapshotService, Clock clock) {
        this.snapshotService = snapshotService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void exportTodaySnapshot() {
        snapshotService.exportSnapshot(LocalDate.now(clock));
    }
}
