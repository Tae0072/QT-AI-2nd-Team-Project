package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.JournalChangedEvent;
import com.qtai.domain.note.api.JournalEventType;
import com.qtai.domain.note.api.NoteStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JournalEventHandlerTest {

    private JournalEventRepository journalEventRepository;
    private JournalEventPersistenceService journalEventPersistenceService;
    private JournalEventHandler handler;

    @BeforeEach
    void setUp() {
        journalEventRepository = mock(JournalEventRepository.class);
        journalEventPersistenceService = mock(JournalEventPersistenceService.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-28T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        handler = new JournalEventHandler(journalEventRepository, journalEventPersistenceService, clock);
    }

    @Test
    @DisplayName("AFTER_COMMIT handler stores pending event and then marks it processed")
    void handle_storesProcessedJournalEvent() {
        JournalChangedEvent event = event(UUID.randomUUID());
        JournalEvent pending = JournalEvent.pending(event);
        when(journalEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(journalEventPersistenceService.savePending(event)).thenReturn(pending);

        handler.handle(event);

        verify(journalEventPersistenceService).savePending(event);
        verify(journalEventPersistenceService).markProcessed(pending, LocalDateTime.of(2026, 5, 28, 12, 0));
        verify(journalEventPersistenceService, never()).markFailed(any(), any(), any());
    }

    @Test
    @DisplayName("same eventId is ignored")
    void handle_duplicateEventIdIgnored() {
        JournalChangedEvent event = event(UUID.randomUUID());
        when(journalEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        handler.handle(event);

        verify(journalEventPersistenceService, never()).savePending(any());
        verify(journalEventPersistenceService, never()).markProcessed(any(), any());
    }

    @Test
    @DisplayName("processing failure stores failed state for retry candidate")
    void handle_processingFailure_marksFailedCandidate() {
        JournalChangedEvent event = event(UUID.randomUUID());
        JournalEvent pending = JournalEvent.pending(event);
        when(journalEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(journalEventPersistenceService.savePending(event)).thenReturn(pending);
        doThrow(new IllegalStateException("processing failed"))
                .when(journalEventPersistenceService)
                .markProcessed(eq(pending), any());

        assertThatThrownBy(() -> handler.handle(event))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("processing failed");

        verify(journalEventPersistenceService).markFailed(pending, "processing failed",
                LocalDateTime.of(2026, 5, 28, 12, 0));
    }

    @Test
    @DisplayName("unique violation for already stored eventId is treated as idempotent success")
    void handle_duplicateEventIdDuringSaveIgnored() {
        JournalChangedEvent event = event(UUID.randomUUID());
        when(journalEventRepository.existsByEventId(event.eventId()))
                .thenReturn(false)
                .thenReturn(true);
        when(journalEventPersistenceService.savePending(event))
                .thenThrow(new DataIntegrityViolationException("duplicate eventId"));

        handler.handle(event);

        verify(journalEventPersistenceService).savePending(event);
        verify(journalEventPersistenceService, never()).markProcessed(any(), any());
        verify(journalEventPersistenceService, never()).markFailed(any(), any(), any());
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
