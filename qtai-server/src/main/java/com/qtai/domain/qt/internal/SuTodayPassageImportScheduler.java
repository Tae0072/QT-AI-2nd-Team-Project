package com.qtai.domain.qt.internal;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import com.qtai.domain.qt.client.sum.SuTodayBibleClient;
import com.qtai.domain.qt.client.sum.SuTodayPassage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class SuTodayPassageImportScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final SuTodayBibleClient client;
    private final QtTodayPassageImportService importService;
    private final QtPassageRepository qtPassageRepository;
    private final Clock clock;
    private final boolean enabled;

    SuTodayPassageImportScheduler(
            SuTodayBibleClient client,
            QtTodayPassageImportService importService,
            QtPassageRepository qtPassageRepository,
            Clock clock,
            @Value("${qt.today-source.sum.enabled:true}") boolean enabled
    ) {
        this.client = client;
        this.importService = importService;
        this.qtPassageRepository = qtPassageRepository;
        this.clock = clock;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    void importTodayOnStartup() {
        if (!enabled) {
            return;
        }
        try {
            LocalDate today = LocalDate.now(clock.withZone(KST));
            if (qtPassageRepository.existsByQtDate(today)) {
                return;
            }
            importToday();
        } catch (RuntimeException exception) {
            LocalDate today = LocalDate.now(clock.withZone(KST));
            log.warn(
                    "성서유니온 오늘 QT startup 보강 실패. qtDate={}, errorType={}, errorMessage={}",
                    today,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    void importToday() {
        if (!enabled) {
            return;
        }
        LocalDate today = LocalDate.now(clock.withZone(KST));
        try {
            SuTodayPassage passage = client.fetchToday();
            QtPassage saved = importService.importToday(today, passage);
            log.info(
                    "성서유니온 오늘 QT 본문 반영 완료. qtDate={}, qtPassageId={}, reference={}",
                    today,
                    saved.getId(),
                    passage.referenceText()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "성서유니온 오늘 QT 본문 반영 실패. qtDate={}, errorType={}, errorMessage={}",
                    today,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }
}
