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
        LocalDate today = LocalDate.now(clock.withZone(KST));
        try {
            if (qtPassageRepository.existsByQtDate(today)) {
                return;
            }
            importToday();
        } catch (RuntimeException exception) {
            log.warn(
                    "성서유니온 오늘 QT startup 보강 실패. qtDate={}, errorType={}, errorMessage={}",
                    today,
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }

    /**
     * 성서유니온 오늘 QT 본문 수집 — 매일 00:02 KST.
     *
     * <p>00:05 AI 해설 시딩(CLAUDE.md §6)과 같은 분(00:05)에 돌면 수집 완료 전에
     * 시딩이 실행될 수 있어(실행 순서 보장 없음) 00:02로 분리한다.
     * 시딩은 "오늘 본문 존재" 전제를 날짜 직접 조회로 확인하고, 본문이 없으면
     * TODAY_QT_PASSAGE_NOT_FOUND로 기록·스킵한다.
     */
    @Scheduled(cron = "0 2 0 * * *", zone = "Asia/Seoul")
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
