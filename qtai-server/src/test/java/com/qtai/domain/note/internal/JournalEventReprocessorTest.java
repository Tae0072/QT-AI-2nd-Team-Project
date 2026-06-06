package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.JournalChangedEvent;
import com.qtai.domain.note.api.JournalEventType;
import com.qtai.domain.note.api.NoteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * JournalEventReprocessor 단위 테스트(P1-10).
 *
 * <p>TransactionTemplate은 mock PlatformTransactionManager로 구동한다 — execute()가 콜백을 동기 실행하므로
 * 실제 DB 없이 상태 전이/백오프/격리 로직을 검증할 수 있다.
 */
class JournalEventReprocessorTest {

    // 2026-05-28 12:00 KST
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-05-28T03:00:00Z"), ZoneId.of("Asia/Seoul"));
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 28, 12, 0);
    private static final int MAX_RETRY = 3;
    private static final long BASE_BACKOFF = 60L;
    private static final long MAX_BACKOFF = 3600L;

    private JournalEventRepository repository;
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        repository = mock(JournalEventRepository.class);
        transactionManager = mock(PlatformTransactionManager.class);
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
    }

    private JournalEventReprocessor reprocessor(List<JournalEventDeliveryHandler> handlers) {
        return new JournalEventReprocessor(
                repository, handlers, transactionManager, CLOCK,
                true, 50, MAX_RETRY, BASE_BACKOFF, MAX_BACKOFF);
    }

    private JournalEvent pendingEvent() {
        return JournalEvent.pending(new JournalChangedEvent(
                UUID.randomUUID(), 10L, 99L, 100L, null,
                JournalEventType.JOURNAL_CREATED, null, NoteStatus.DRAFT, NOW.minusMinutes(5)));
    }

    @Test
    @DisplayName("PENDING 이벤트를 처리하면 PROCESSED로 전이한다")
    void pendingBecomesProcessed() {
        JournalEvent event = pendingEvent();
        when(repository.findDueForReprocess(eq(NOW), eq(MAX_RETRY), any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(repository.findById(1L)).thenReturn(Optional.of(event));

        int processed = reprocessor(List.of()).runDueBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(JournalEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isEqualTo(NOW);
        assertThat(event.getNextAttemptAt()).isNull();
    }

    @Test
    @DisplayName("핸들러가 예외를 던지면 FAILED로 기록하고 백오프(base)를 예약한다")
    void handlerFailureMarksFailedWithBackoff() {
        JournalEvent event = pendingEvent();
        when(repository.findDueForReprocess(any(), eq(MAX_RETRY), any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(repository.findById(1L)).thenReturn(Optional.of(event));
        List<JournalEventDeliveryHandler> handlers = List.of(e -> {
            throw new IllegalStateException("boom");
        });

        int processed = reprocessor(handlers).runDueBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(JournalEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getLastErrorMessage()).isEqualTo("boom");
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(BASE_BACKOFF)); // 2^0 * base
    }

    @Test
    @DisplayName("재시도일수록 백오프가 지수로 늘어난다(2^retryCount * base)")
    void backoffGrowsExponentially() {
        JournalEvent event = pendingEvent();
        event.markFailedForRetry("first", NOW.minusHours(1), NOW.minusMinutes(1)); // retryCount=1, due
        when(repository.findDueForReprocess(any(), eq(MAX_RETRY), any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(repository.findById(1L)).thenReturn(Optional.of(event));
        List<JournalEventDeliveryHandler> handlers = List.of(e -> {
            throw new IllegalStateException("again");
        });

        reprocessor(handlers).runDueBatch();

        assertThat(event.getRetryCount()).isEqualTo(2);
        assertThat(event.getNextAttemptAt()).isEqualTo(NOW.plusSeconds(BASE_BACKOFF * 2)); // 2^1 * base
    }

    @Test
    @DisplayName("이미 PROCESSED면 다른 주기가 처리한 것으로 보고 건너뛴다(멱등)")
    void alreadyProcessedIsSkipped() {
        JournalEvent event = pendingEvent();
        event.markProcessed(NOW.minusMinutes(1));
        when(repository.findDueForReprocess(any(), eq(MAX_RETRY), any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(repository.findById(1L)).thenReturn(Optional.of(event));

        int processed = reprocessor(List.of()).runDueBatch();

        assertThat(processed).isZero();
        assertThat(event.getStatus()).isEqualTo(JournalEventStatus.PROCESSED);
    }

    @Test
    @DisplayName("재시도 상한에 도달한 FAILED는 더는 처리하지 않는다(dead-letter 보존)")
    void failedAtCapIsNotReprocessed() {
        JournalEvent event = pendingEvent();
        event.markFailedForRetry("e1", NOW.minusHours(3), NOW.minusHours(2));
        event.markFailedForRetry("e2", NOW.minusHours(2), NOW.minusHours(1));
        event.markFailedForRetry("e3", NOW.minusMinutes(90), NOW.minusMinutes(1)); // retryCount=3 == MAX
        when(repository.findDueForReprocess(any(), eq(MAX_RETRY), any(Pageable.class)))
                .thenReturn(List.of(1L));
        when(repository.findById(1L)).thenReturn(Optional.of(event));

        int processed = reprocessor(List.of()).runDueBatch();

        assertThat(processed).isZero();
        assertThat(event.getStatus()).isEqualTo(JournalEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(MAX_RETRY); // 증가하지 않음
    }

    @Test
    @DisplayName("한 이벤트의 실패가 같은 배치의 다른 이벤트 처리를 막지 않는다(이벤트별 격리)")
    void perEventIsolation() {
        JournalEvent failing = pendingEvent();
        JournalEvent succeeding = pendingEvent();
        when(repository.findDueForReprocess(any(), eq(MAX_RETRY), any(Pageable.class)))
                .thenReturn(List.of(1L, 2L));
        when(repository.findById(1L)).thenReturn(Optional.of(failing));
        when(repository.findById(2L)).thenReturn(Optional.of(succeeding));
        // 1번 이벤트(noteId 기준 구분 불가하므로) — 핸들러가 첫 호출에만 던지도록 구성
        List<JournalEventDeliveryHandler> handlers = List.of(e -> {
            if (e == failing) {
                throw new IllegalStateException("boom");
            }
        });

        int processed = reprocessor(handlers).runDueBatch();

        assertThat(processed).isEqualTo(2); // 둘 다 처리(하나는 실패기록, 하나는 성공)
        assertThat(failing.getStatus()).isEqualTo(JournalEventStatus.FAILED);
        assertThat(succeeding.getStatus()).isEqualTo(JournalEventStatus.PROCESSED);
    }
}
