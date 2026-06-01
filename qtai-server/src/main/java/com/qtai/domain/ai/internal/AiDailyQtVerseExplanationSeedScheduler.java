package com.qtai.domain.ai.internal;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class AiDailyQtVerseExplanationSeedScheduler {

    private final AiDailyQtVerseExplanationSeedService service;
    private final boolean enabled;

    AiDailyQtVerseExplanationSeedScheduler(
            AiDailyQtVerseExplanationSeedService service,
            @Value("${ai.daily-qt-verse-seed.enabled:true}") boolean enabled
    ) {
        this.service = service;
        this.enabled = enabled;
    }

    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Seoul")
    void seedDaily() {
        if (!enabled) {
            return;
        }
        try {
            int createdCount = service.seedToday();
            log.info("AI daily QT verse explanation seed completed. createdCount={}", createdCount);
        } catch (RuntimeException exception) {
            log.warn(
                    "AI daily QT verse explanation seed failed. errorType={}, errorMessage={}",
                    exception.getClass().getSimpleName(),
                    exception.getMessage()
            );
        }
    }
}
