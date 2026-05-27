package com.qtai.domain.note.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;
import com.qtai.domain.note.api.dto.CreateNoteCommand;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import com.qtai.domain.note.api.dto.NoteListResponse;
import com.qtai.domain.note.api.dto.NoteSaveResponse;
import com.qtai.domain.note.api.dto.UpdateNoteCommand;
import com.qtai.domain.note.client.qt.NoteQtClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NoteServiceTest {

    private NoteRepository noteRepository;
    private NoteVerseRepository noteVerseRepository;
    private GetBibleVerseUseCase getBibleVerseUseCase;
    private NoteQtClient noteQtClient;
    private NoteService noteService;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        noteVerseRepository = mock(NoteVerseRepository.class);
        getBibleVerseUseCase = mock(GetBibleVerseUseCase.class);
        noteQtClient = mock(NoteQtClient.class);
        noteService = new NoteService(noteRepository, noteVerseRepository, getBibleVerseUseCase, noteQtClient);
        pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    @Test
    @DisplayName("list returns own active notes and real visibility fields")
    void list_mapsNoteFields() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.SAVED, null);
        when(noteRepository.search(eq(10L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(note), pageable, 1L));
        when(noteVerseRepository.findAllByNoteIdInOrderByNoteIdAscDisplayOrderAsc(List.of(1L)))
                .thenReturn(List.of());

        NoteListResponse response = noteService.list(10L, null, null, null, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).visibility()).isEqualTo(NoteVisibility.PRIVATE);
        assertThat(response.sort()).isEqualTo("updatedAt,desc");
    }

    @Test
    @DisplayName("create meditation note requires unique active QT note")
    void create_duplicateMeditation_rejected() {
        when(noteRepository.existsByMemberIdAndQtPassageIdAndCategoryAndActiveUniqueKey(
                10L, 100L, NoteCategory.MEDITATION, Note.ACTIVE_KEY))
                .thenReturn(true);

        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.MEDITATION, 100L, "묵상", "본문", null, null, null, null,
                List.of(), NoteStatus.DRAFT, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.create(10L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_NOTE);
        verify(noteRepository, never()).save(any());
    }

    @Test
    @DisplayName("create sermon note requires at least one verse")
    void create_sermonWithoutVerse_rejected() {
        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.SERMON, null, "설교", "본문", null, null, null, null,
                List.of(), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.create(10L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("create stores unique verse ids in request order")
    void create_replacesVersesWithDeduplicatedOrder() {
        when(getBibleVerseUseCase.getVerse(any()))
                .thenReturn(new BibleVerseResponse(1L, "GEN", 1, 1, "중립 예시 문구", null));
        when(noteRepository.save(any())).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            setField(note, "id", 99L);
            return note;
        });

        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.PRAYER, null, "기도", "본문", null, null, null, null,
                List.of(3L, 3L, 2L), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        NoteSaveResponse response = noteService.create(10L, command);

        assertThat(response.id()).isEqualTo(99L);
        verify(noteVerseRepository).deleteByNoteId(99L);
        verify(noteVerseRepository).saveAll(any());
        verify(getBibleVerseUseCase).getVerse(3L);
        verify(getBibleVerseUseCase).getVerse(2L);
    }

    @Test
    @DisplayName("draft lookup returns exists false when no meditation draft")
    void getDraft_missing_returnsFalse() {
        when(noteRepository.findDraft(10L, NoteCategory.MEDITATION, 100L))
                .thenReturn(Optional.empty());

        NoteDraftResponse response = noteService.getDraft(10L, NoteCategory.MEDITATION, 100L);

        assertThat(response.exists()).isFalse();
        assertThat(response.note()).isNull();
    }

    @Test
    @DisplayName("update deleted note is invalid status transition")
    void update_deletedNote_rejected() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.SAVED, null);
        note.delete(java.time.LocalDateTime.now());
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        UpdateNoteCommand command = new UpdateNoteCommand(
                NoteCategory.PRAYER, null, "기도", "본문", null, null, null, null,
                List.of(), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.update(10L, 1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("Note entity rejects deleted note update with BusinessException")
    void note_updateDeletedNote_throwsBusinessException() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.SAVED, null);
        note.delete(java.time.LocalDateTime.now());

        assertThatThrownBy(() -> note.update(
                NoteCategory.PRAYER,
                null,
                NoteStatus.SAVED,
                NoteVisibility.PRIVATE,
                "title",
                "body",
                null,
                null,
                null,
                null,
                java.time.LocalDateTime.now()
        ))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_STATUS_TRANSITION);
    }

    @Test
    @DisplayName("delete marks status deleted and clears active key")
    void delete_softDeletesNote() {
        Note note = persistedNote(1L, 10L, NoteCategory.MEDITATION, NoteStatus.SAVED, 100L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        noteService.delete(10L, 1L);

        assertThat(note.getStatus()).isEqualTo(NoteStatus.DELETED);
        assertThat(note.getDeletedAt()).isNotNull();
        assertThat(note.getActiveUniqueKey()).isNull();
    }

    private static Note persistedNote(Long id, Long memberId, NoteCategory category, NoteStatus status, Long qtPassageId) {
        Note note = Note.create(memberId, qtPassageId, category, status, NoteVisibility.PRIVATE,
                "제목", "본문", null, null, null, null, java.time.LocalDateTime.now());
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
