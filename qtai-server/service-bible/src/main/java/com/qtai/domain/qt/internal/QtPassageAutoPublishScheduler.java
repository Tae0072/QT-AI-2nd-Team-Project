package com.qtai.domain.qt.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 미게시 자동수집 QT 본문을 게시 시각(QT 날짜 04:00 KST)에 자동게시한다.
 *
 * <p>매일 04:00 정기 실행 + 기동 시 catch-up(서버가 04:00에 안 돌아 누락된 게시 보강)이 같은 로직을
 * 공유한다({@link QtPassageAutoPublishService#publishDue}). {@code qt.auto-publish.enabled=false}이면
 * 빈이 생성되지 않는다(테스트는 false로 두어 기동 시 자동게시가 일어나지 않게 한다).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "qt.auto-publish", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
class QtPassageAutoPublishScheduler {

    private final QtPassageAutoPublishService autoPublishService;

    /** 매일 04:00 KST 자동게시. */
    @Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
    void publishAt0400() {
        runSafely("scheduled-0400");
    }

    /** 기동 시 catch-up — 04:00이 이미 지난 미게시 본문(누락분)을 그 날짜 04:00으로 게시한다. */
    @EventListener(ApplicationReadyEvent.class)
    void catchUpOnStartup() {
        runSafely("startup-catchup");
    }

    private void runSafely(String trigger) {
        try {
            int published = autoPublishService.publishDue();
            if (published > 0) {
                log.info("QT 자동게시 완료. trigger={}, publishedCount={}", trigger, published);
            }
        } catch (RuntimeException exception) {
            log.warn("QT 자동게시 실패. trigger={}, errorType={}, errorMessage={}",
                    trigger, exception.getClass().getSimpleName(), exception.getMessage());
        }
    }
}
