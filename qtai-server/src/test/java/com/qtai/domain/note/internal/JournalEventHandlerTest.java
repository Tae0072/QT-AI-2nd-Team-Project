package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.internal.event.JournalChangedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(OutputCaptureExtension.class)
class JournalEventHandlerTest {

    private final JournalEventRepository journalEventRepository = mock(JournalEventRepository.class);
    private final JournalEventHandler handler = new JournalEventHandler(journalEventRepository);

    @Test
    @DisplayName("AFTER_COMMIT 이벤트를 journal_events 엔티티로 저장한다")
    void handle_savesJournalEvent() {
        JournalChangedEvent event = event();
        when(journalEventRepository.existsByEventId(event.eventId())).thenReturn(false);

        handler.handle(event);

        verify(journalEventRepository).save(any(JournalEvent.class));
    }

    @Test
    @DisplayName("동일 eventId 중복 수신은 멱등 성공 처리한다")
    void handle_duplicateEventId_returnsWithoutSave() {
        JournalChangedEvent event = event();
        when(journalEventRepository.existsByEventId(event.eventId())).thenReturn(true);

        handler.handle(event);

        verify(journalEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("unique 제약 위반 후 eventId가 존재하면 멱등 성공 처리한다")
    void handle_integrityViolationWithExistingEvent_returnsWithoutFailureLog(CapturedOutput output) {
        JournalChangedEvent event = event();
        when(journalEventRepository.existsByEventId(event.eventId()))
                .thenReturn(false)
                .thenReturn(true);
        when(journalEventRepository.save(any(JournalEvent.class)))
                .thenThrow(new DataIntegrityViolationException("uk_journal_events_event_id"));

        handler.handle(event);

        assertThat(output.getAll()).doesNotContain("journal event handler failed");
    }

    @Test
    @DisplayName("저장 실패 로그에는 eventId, event type, handler, error만 남긴다")
    void handle_saveFailure_logsSafeFields(CapturedOutput output) {
        JournalChangedEvent event = event();
        when(journalEventRepository.existsByEventId(event.eventId())).thenReturn(false);
        when(journalEventRepository.save(any(JournalEvent.class)))
                .thenThrow(new IllegalStateException("storage unavailable"));

        handler.handle(event);

        assertThat(output.getAll())
                .contains("journal event handler failed")
                .contains(event.eventId())
                .contains("JOURNAL_UPDATED")
                .contains("JournalEventHandler")
                .contains("storage unavailable")
                .doesNotContain("본문");
    }

    private JournalChangedEvent event() {
        return new JournalChangedEvent(
                "11111111-1111-1111-1111-111111111111",
                JournalEventType.JOURNAL_UPDATED,
                10L,
                99L,
                100L,
                NoteCategory.MEDITATION,
                NoteStatus.DRAFT,
                NoteStatus.SAVED,
                LocalDate.of(2026, 5, 17),
                LocalDateTime.of(2026, 5, 17, 9, 0)
        );
    }
}
