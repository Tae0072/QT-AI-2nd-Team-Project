package com.qtai.domain.note.internal;

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;
import com.qtai.domain.note.api.dto.MeditationCalendarResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MeditationCalendarServiceTest {

    private NoteRepository noteRepository;
    private MeditationCalendarService service;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        Clock clock = Clock.fixed(Instant.parse("2026-05-28T03:00:00Z"), ZoneId.of("Asia/Seoul"));
        service = new MeditationCalendarService(noteRepository, clock);
    }

    @Test
    @DisplayName("saved notes are grouped by month days")
    void getCalendar_groupsSavedNotesByDay() {
        YearMonth month = YearMonth.of(2026, 5);
        Note meditation = note(1L, NoteCategory.MEDITATION, LocalDateTime.of(2026, 5, 17, 9, 0));
        Note prayer = note(2L, NoteCategory.PRAYER, LocalDateTime.of(2026, 5, 17, 10, 0));
        Note gratitude = note(3L, NoteCategory.GRATITUDE, LocalDateTime.of(2026, 5, 28, 8, 0));
        when(noteRepository.findSavedCalendarNotes(
                10L,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 6, 1, 0, 0)))
                .thenReturn(List.of(meditation, prayer, gratitude));

        MeditationCalendarResponse response = service.getCalendar(10L, month);

        assertThat(response.month()).isEqualTo("2026-05");
        assertThat(response.days()).hasSize(31);
        MeditationCalendarResponse.Day may17 = response.days().get(16);
        assertThat(may17.saved()).isTrue();
        assertThat(may17.savedNoteCount()).isEqualTo(2);
        assertThat(may17.meditationNoteId()).isEqualTo(1L);
        assertThat(may17.categories()).containsExactly(NoteCategory.MEDITATION, NoteCategory.PRAYER);
        assertThat(response.summary().savedDays()).isEqualTo(2);
        assertThat(response.summary().savedNoteCount()).isEqualTo(3);
        verify(noteRepository).findSavedCalendarNotes(
                10L,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 6, 1, 0, 0));
    }

    @Test
    @DisplayName("streak is counted backward from earlier of month end and KST today")
    void getCalendar_calculatesStreak() {
        YearMonth month = YearMonth.of(2026, 5);
        when(noteRepository.findSavedCalendarNotes(
                10L,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 6, 1, 0, 0)))
                .thenReturn(List.of(
                        note(1L, NoteCategory.MEDITATION, LocalDateTime.of(2026, 5, 26, 9, 0)),
                        note(2L, NoteCategory.MEDITATION, LocalDateTime.of(2026, 5, 27, 9, 0)),
                        note(3L, NoteCategory.PRAYER, LocalDateTime.of(2026, 5, 28, 9, 0))
                ));

        MeditationCalendarResponse response = service.getCalendar(10L, month);

        assertThat(response.summary().meditationStreakDays()).isEqualTo(3);
    }

    @Test
    @DisplayName("empty days are not created as records")
    void getCalendar_emptyDaysRemainEmpty() {
        YearMonth month = YearMonth.of(2026, 5);
        when(noteRepository.findSavedCalendarNotes(
                10L,
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 6, 1, 0, 0)))
                .thenReturn(List.of());

        MeditationCalendarResponse response = service.getCalendar(10L, month);

        assertThat(response.days()).allMatch(day -> !day.saved());
        assertThat(response.summary().savedDays()).isZero();
        assertThat(response.summary().savedNoteCount()).isZero();
        assertThat(response.summary().meditationStreakDays()).isZero();
    }

    private static Note note(Long id, NoteCategory category, LocalDateTime savedAt) {
        Note note = Note.create(
                10L,
                category == NoteCategory.MEDITATION ? 100L + id : null,
                category,
                NoteStatus.SAVED,
                NoteVisibility.PRIVATE,
                "제목",
                "본문",
                null,
                null,
                null,
                null,
                savedAt
        );
        setField(note, "id", id);
        return note;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Field findField(Class<?> type, String fieldName) throws NoSuchFieldException {
        Class<?> current = type;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
