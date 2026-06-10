package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.JournalChangedEvent;
import com.qtai.domain.note.api.JournalEventType;
import com.qtai.domain.note.api.NoteStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

/**
 * journal_events 아웃박스 재처리기 단위 테스트(P1-10).
 *
 * <p>핵심 검증: 전달 핸들러가 실패하면 이벤트가 유실되지 않고 <b>FAILED + 백오프 예약 + retryCount 증가</b>로
 * 남아 "재처리 가능 상태"가 보존된다(CLAUDE.md §10). 성공 시에는 PROCESSED로 전이한다.
 */
@ExtendWith(MockitoExtension.class)
class JournalEventReprocessorTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private JournalEventRepository journalEventRepository;
    @Mock
    private PlatformTransactionManager transactionManager;

    private JournalEvent pendingEvent() {
        return JournalEvent.pending(new JournalChangedEvent(
                UUID.randomUUID(), 1L, 10L, 100L, null,
                JournalEventType.JOURNAL_CREATED, null, NoteStatus.DRAFT,
                LocalDateTime.now(CLOCK)));
    }

    private JournalEventReprocessor reprocessor(List<JournalEventDeliveryHandler> handlers) {
        // 트랜잭션 템플릿이 콜백을 그대로 실행하도록 mock 트랜잭션을 돌려준다.
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        return new JournalEventReprocessor(
                journalEventRepository, handlers, transactionManager, CLOCK,
                true, 50, 10, 60, 3600);
    }

    @Test
    void 핸들러_실패시_FAILED_재처리가능_상태로_보존된다() {
        JournalEvent event = pendingEvent();
        when(journalEventRepository.findDueForReprocess(any(), anyInt(), any())).thenReturn(List.of(1L));
        when(journalEventRepository.findById(1L)).thenReturn(Optional.of(event));
        JournalEventDeliveryHandler failing = e -> {
            throw new RuntimeException("boom");
        };

        int processed = reprocessor(List.of(failing)).runDueBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(JournalEventStatus.FAILED);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getNextAttemptAt()).isNotNull();      // 백오프 재시도 예약 = 재처리 가능
        assertThat(event.getLastErrorMessage()).contains("boom");
        assertThat(event.getProcessedAt()).isNull();
    }

    @Test
    void 핸들러_성공시_PROCESSED로_전이된다() {
        JournalEvent event = pendingEvent();
        when(journalEventRepository.findDueForReprocess(any(), anyInt(), any())).thenReturn(List.of(1L));
        when(journalEventRepository.findById(1L)).thenReturn(Optional.of(event));
        JournalEventDeliveryHandler ok = e -> { /* 정상 전달 */ };

        int processed = reprocessor(List.of(ok)).runDueBatch();

        assertThat(processed).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(JournalEventStatus.PROCESSED);
        assertThat(event.getProcessedAt()).isNotNull();
        assertThat(event.getRetryCount()).isZero();
    }
}
