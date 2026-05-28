package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.JournalChangedEvent;
import com.qtai.domain.note.api.JournalEventType;
import com.qtai.domain.note.api.NoteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JournalEventHandlerTest {

    private JournalEventRepository journalEventRepository;
    private JournalEventHandler handler;

    @BeforeEach
    void setUp() {
        journalEventRepository = mock(JournalEventRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-28T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        handler = new JournalEventHandler(journalEventRepository, clock);
    }

    @Test
    @DisplayName("AFTER_COMMIT handler stores processed journal event")
    void handle_storesProcessedJournalEvent() {
        ArgumentCaptor<JournalEvent> captor = ArgumentCaptor.forClass(JournalEvent.class);
        JournalChangedEvent event = event(UUID.randomUUID());
        when(journalEventRepository.existsByEventId(event.eventId())).thenReturn(false);

        handler.handle(event);

        verify(journalEventRepository).save(captor.capture());
        JournalEvent saved = captor.getValue();
        assertThat(saved.getEventId()).isEqualTo(event.eventId());
        assertThat(saved.getEventType()).isEqualTo(JournalEventType.JOURNAL_CREATED);
        assertThat(saved.getStatus()).isEqualTo(JournalEventStatus.PROCESSED);
        assertThat(saved.getProcessedAt()).isEqualTo(LocalDateTime.of(2026, 5, 28, 12, 0));
    }

    @Test
    @DisplayName("same eventId is ignored")
    void handle_duplicateEventIdIgnored() {
        JournalChangedEvent event = event(UUID.randomUUID());
        when(journalEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        handler.handle(event);

        verify(journalEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("handler failure leaves failed state on event candidate")
    void handle_saveFailure_marksFailedCandidate() {
        ArgumentCaptor<JournalEvent> captor = ArgumentCaptor.forClass(JournalEvent.class);
        JournalChangedEvent event = event(UUID.randomUUID());
        when(journalEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        doThrow(new IllegalStateException("저장 실패")).when(journalEventRepository).save(any());

        assertThatThrownBy(() -> handler.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("저장 실패");

        verify(journalEventRepository).save(captor.capture());
        JournalEvent failed = captor.getValue();
        assertThat(failed.getStatus()).isEqualTo(JournalEventStatus.FAILED);
        assertThat(failed.getRetryCount()).isEqualTo(1);
        assertThat(failed.getLastErrorMessage()).isEqualTo("저장 실패");
        assertThat(failed.getFailedAt()).isEqualTo(LocalDateTime.of(2026, 5, 28, 12, 0));
    }

    private static JournalChangedEvent event(UUID eventId) {
        return new JournalChangedEvent(
                eventId,
                10L,
                99L,
                100L,
                JournalEventType.JOURNAL_CREATED,
                null,
                NoteStatus.DRAFT,
                LocalDateTime.of(2026, 5, 28, 12, 0)
        );
    }
}
