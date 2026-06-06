package com.qtai.domain.note.internal;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * journal_events 아웃박스 재처리기(P1-10, 리뷰 H-13 보강).
 *
 * <p>역할 1 — <b>PENDING 드레인</b>: {@link JournalEventOutbox}가 노트 트랜잭션과 함께 원자적으로 적재한
 * PENDING 이벤트를 폴링해 처리(PROCESSED 전이)한다. 적재만 in-tx로 끝내고 처리는 여기서 비동기로 하므로
 * 처리 실패가 사용자 응답(500)으로 전파되지 않는다. 크래시로 처리 전에 멈춰도 PENDING 행이 남아 다음 폴링에 복구된다.
 *
 * <p>역할 2 — <b>FAILED 재시도</b>: 처리 실패는 {@code FAILED + nextAttemptAt(백오프)}로 기록하고, 도래 시
 * 재시도 상한까지 다시 집어 든다. 상한 도달 시 자동 재시도를 멈추되 상태/원인/retryCount를 보존해
 * 운영 가시성과 재처리 가능 상태(수동 재큐)를 남긴다.
 *
 * <p>이벤트마다 독립 트랜잭션(REQUIRES_NEW 효과의 {@link TransactionTemplate})으로 처리해, 한 이벤트의
 * 실패가 같은 배치의 다른 이벤트로 전파되지 않는다(AI 생성 워커와 동일한 격리 방식).
 */
@Slf4j
@Component
class JournalEventReprocessor {

    static final String HANDLER_NAME = "JournalEventReprocessor";

    private final JournalEventRepository journalEventRepository;
    private final List<JournalEventDeliveryHandler> deliveryHandlers;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;
    private final boolean enabled;
    private final int batchSize;
    private final int maxRetry;
    private final long baseBackoffSeconds;
    private final long maxBackoffSeconds;

    JournalEventReprocessor(
            JournalEventRepository journalEventRepository,
            List<JournalEventDeliveryHandler> deliveryHandlers,
            PlatformTransactionManager transactionManager,
            Clock clock,
            @Value("${journal.reprocessor.enabled:true}") boolean enabled,
            @Value("${journal.reprocessor.batch-size:50}") int batchSize,
            @Value("${journal.reprocessor.max-retry:10}") int maxRetry,
            @Value("${journal.reprocessor.base-backoff-seconds:60}") long baseBackoffSeconds,
            @Value("${journal.reprocessor.max-backoff-seconds:3600}") long maxBackoffSeconds
    ) {
        this.journalEventRepository = journalEventRepository;
        this.deliveryHandlers = deliveryHandlers;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.enabled = enabled;
        this.batchSize = batchSize;
        this.maxRetry = maxRetry;
        this.baseBackoffSeconds = baseBackoffSeconds;
        this.maxBackoffSeconds = maxBackoffSeconds;
    }

    @Scheduled(fixedDelayString = "${journal.reprocessor.fixed-delay-ms:10000}")
    void poll() {
        if (!enabled) {
            return;
        }
        try {
            int processed = runDueBatch();
            if (processed > 0) {
                log.info("journal reprocessor processed events. processedCount={}", processed);
            }
        } catch (RuntimeException e) {
            // 폴링 자체 실패(예: 조회 단계)는 다음 주기에 재시도한다. 개별 이벤트 실패는 processOne에서 격리.
            log.warn("journal reprocessor poll failed. errorType={}, errorMessage={}",
                    e.getClass().getSimpleName(), e.getMessage());
        }
    }

    int runDueBatch() {
        List<Long> dueIds = journalEventRepository.findDueForReprocess(
                LocalDateTime.now(clock), maxRetry, PageRequest.of(0, Math.max(batchSize, 1)));
        int processed = 0;
        for (Long id : dueIds) {
            if (processOne(id)) {
                processed++;
            }
        }
        return processed;
    }

    private boolean processOne(Long id) {
        return Boolean.TRUE.equals(transactionTemplate.execute(status -> {
            JournalEvent event = journalEventRepository.findById(id).orElse(null);
            if (event == null || !isDue(event)) {
                return false; // 이미 다른 주기가 처리했거나 더는 대상 아님
            }
            try {
                deliver(event);
                event.markProcessed(LocalDateTime.now(clock));
                return true;
            } catch (RuntimeException e) {
                LocalDateTime now = LocalDateTime.now(clock);
                event.markFailedForRetry(e.getMessage(), now, now.plusSeconds(backoffSeconds(event.getRetryCount())));
                log.error("journal event reprocess failed. eventId={}, eventType={}, handlerName={}, errorMessage={}",
                        event.getEventId(), event.getEventType(), HANDLER_NAME, e.getMessage(), e);
                return true;
            }
        }));
    }

    private void deliver(JournalEvent event) {
        for (JournalEventDeliveryHandler handler : deliveryHandlers) {
            handler.deliver(event);
        }
    }

    private boolean isDue(JournalEvent event) {
        if (event.getStatus() == JournalEventStatus.PENDING) {
            return true;
        }
        if (event.getStatus() != JournalEventStatus.FAILED || event.getRetryCount() >= maxRetry) {
            return false;
        }
        LocalDateTime nextAttemptAt = event.getNextAttemptAt();
        return nextAttemptAt == null || !nextAttemptAt.isAfter(LocalDateTime.now(clock));
    }

    /** 지수 백오프: base * 2^retryCount, 단 max 상한. retryCount는 직전까지 실패 횟수. */
    private long backoffSeconds(int retryCount) {
        long capExponent = Math.min(retryCount, 16); // 2^16 이상은 오버플로/무의미 — 어차피 max로 캡
        long backoff = baseBackoffSeconds << capExponent;
        if (backoff <= 0 || backoff > maxBackoffSeconds) {
            return maxBackoffSeconds;
        }
        return backoff;
    }
}
