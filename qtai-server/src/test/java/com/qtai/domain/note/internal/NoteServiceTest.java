package com.qtai.domain.note.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.NoteVisibility;
import com.qtai.domain.note.api.dto.CreateNoteCommand;
import com.qtai.domain.note.api.dto.NoteCategoryResponse;
import com.qtai.domain.note.api.dto.NoteDetailResponse;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import com.qtai.domain.note.api.dto.NoteListResponse;
import com.qtai.domain.note.api.dto.NoteSaveResponse;
import com.qtai.domain.note.api.dto.UpdateNoteCommand;
import com.qtai.domain.note.client.qt.NoteQtClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
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
    private ArgumentCaptor<Iterable<NoteVerse>> noteVersesCaptor;
    private Pageable pageable;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        noteVerseRepository = mock(NoteVerseRepository.class);
        getBibleVerseUseCase = mock(GetBibleVerseUseCase.class);
        noteQtClient = mock(NoteQtClient.class);
        noteService = new NoteService(noteRepository, noteVerseRepository, getBibleVerseUseCase, noteQtClient);
        noteVersesCaptor = ArgumentCaptor.forClass(Iterable.class);
        when(noteRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            setField(note, "id", 99L);
            return note;
        });
        pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    @Test
    @DisplayName("list returns own active notes and real visibility fields")
    void list_mapsNoteFields() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.SAVED, null);
        when(noteRepository.search(eq(10L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(note), pageable, 1L));

        NoteListResponse response = noteService.list(10L, null, null, null, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).visibility()).isEqualTo(NoteVisibility.PRIVATE);
        assertThat(response.sort()).isEqualTo("updatedAt,desc");
    }

    @Test
    @DisplayName("list returns empty page metadata when repository has no result")
    void list_emptyResult_mapsPageMetadata() {
        Pageable emptyPageable = PageRequest.of(1, 10);
        when(noteRepository.search(eq(10L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), emptyPageable, 0L));

        NoteListResponse response = noteService.list(10L, null, null, "   ", emptyPageable);

        assertThat(response.content()).isEmpty();
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(10);
        assertThat(response.totalElements()).isZero();
        assertThat(response.sort()).isEqualTo("updatedAt,desc");
        verify(noteRepository).search(eq(10L), isNull(), isNull(), isNull(), eq(emptyPageable));
    }

    @Test
    @DisplayName("list escapes LIKE wildcard characters in q")
    void list_escapesLikeWildcards() {
        when(noteRepository.search(eq(10L), isNull(), isNull(), eq("\\%\\_\\\\"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

        noteService.list(10L, null, null, "%_\\", pageable);

        verify(noteRepository).search(eq(10L), isNull(), isNull(), eq("\\%\\_\\\\"), eq(pageable));
    }

    @Test
    @DisplayName("list reports the first requested sort when multiple sort fields are supplied")
    void list_multipleSort_usesFirstSortField() {
        Pageable multiSort = PageRequest.of(0, 20, Sort.by(
                Sort.Order.asc("title"),
                Sort.Order.desc("updatedAt")
        ));
        when(noteRepository.search(eq(10L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), multiSort, 0L));

        NoteListResponse response = noteService.list(10L, null, null, null, multiSort);

        assertThat(response.sort()).isEqualTo("title,asc");
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
    @DisplayName("create meditation draft stores active key private visibility and no savedAt")
    void create_meditationDraft_setsLifecycleFields() {
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.MEDITATION, 100L, "묵상", "본문", null, null, null, null,
                List.of(), NoteStatus.DRAFT, null);

        NoteSaveResponse response = noteService.create(10L, command);

        assertThat(response.status()).isEqualTo(NoteStatus.DRAFT);
        verify(noteQtClient).validateReadable(10L, 100L);
        verify(noteRepository).saveAndFlush(noteCaptor.capture());
        Note saved = noteCaptor.getValue();
        assertThat(saved.getVisibility()).isEqualTo(NoteVisibility.PRIVATE);
        assertThat(saved.getActiveUniqueKey()).isEqualTo(Note.ACTIVE_KEY);
        assertThat(saved.getSavedAt()).isNull();
    }

    @Test
    @DisplayName("create meditation saved note records savedAt")
    void create_meditationSaved_recordsSavedAt() {
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.MEDITATION, 100L, "묵상", "본문", null, null, null, null,
                List.of(), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        NoteSaveResponse response = noteService.create(10L, command);

        assertThat(response.status()).isEqualTo(NoteStatus.SAVED);
        verify(noteRepository).saveAndFlush(noteCaptor.capture());
        assertThat(noteCaptor.getValue().getActiveUniqueKey()).isEqualTo(Note.ACTIVE_KEY);
        assertThat(noteCaptor.getValue().getSavedAt()).isNotNull();
    }

    @Test
    @DisplayName("create meditation accepts one non-blank section without title or body")
    void create_meditationWithOnlySection_succeeds() {
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.MEDITATION, 100L, " ", " ", null, "해석", null, null,
                List.of(), NoteStatus.DRAFT, NoteVisibility.PRIVATE);

        NoteSaveResponse response = noteService.create(10L, command);

        assertThat(response.status()).isEqualTo(NoteStatus.DRAFT);
        verify(noteRepository).saveAndFlush(noteCaptor.capture());
        Note saved = noteCaptor.getValue();
        assertThat(saved.getTitle()).isEmpty();
        assertThat(saved.getBody()).isEmpty();
        assertThat(saved.getInterpretSection()).isEqualTo("해석");
    }

    @Test
    @DisplayName("create meditation rejects unreadable QT passage")
    void create_meditationUnreadableQtPassage_rejected() {
        doThrow(new BusinessException(ErrorCode.NOTE_NOT_FOUND))
                .when(noteQtClient).validateReadable(10L, 404L);
        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.MEDITATION, 404L, "묵상", "본문", null, null, null, null,
                List.of(), NoteStatus.DRAFT, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.create(10L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
        verify(noteRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("create rejects deleted status request")
    void create_deletedStatus_rejected() {
        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.MEDITATION, 100L, "묵상", "본문", null, null, null, null,
                List.of(), NoteStatus.DELETED, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.create(10L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(noteQtClient, never()).validateReadable(any(), any());
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
        when(getBibleVerseUseCase.getVerses(List.of(3L, 2L)))
                .thenReturn(List.of(
                        new BibleVerseResponse(3L, "GEN", 1, 3, "중립 예시 문구", null),
                        new BibleVerseResponse(2L, "GEN", 1, 2, "중립 예시 문구", null)
                ));
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
        verify(getBibleVerseUseCase).getVerses(List.of(3L, 2L));
        verify(getBibleVerseUseCase, never()).getVerse(any());
    }

    @Test
    @DisplayName("create does not map note verse integrity errors to duplicate meditation note")
    void create_noteVerseIntegrityError_notMappedToDuplicateNote() {
        when(getBibleVerseUseCase.getVerses(List.of(3L)))
                .thenReturn(List.of(new BibleVerseResponse(3L, "GEN", 1, 3, "중립 예시 문구", null)));
        DataIntegrityViolationException integrityException =
                new DataIntegrityViolationException("uk_note_verses_note_verse");
        doThrow(integrityException).when(noteVerseRepository).deleteByNoteId(99L);

        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.PRAYER, null, "기도", "본문", null, null, null, null,
                List.of(3L), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.create(10L, command))
                .isSameAs(integrityException);
    }

    @Test
    @DisplayName("get returns detail with verses fetched in one batch")
    void get_existingNote_returnsDetailWithBatchVerses() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.SAVED, null);
        when(noteRepository.findActiveByIdAndMemberId(1L, 10L)).thenReturn(Optional.of(note));
        when(noteVerseRepository.findAllByNoteIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of(
                        NoteVerse.create(1L, 3L, (short) 1),
                        NoteVerse.create(1L, 2L, (short) 2)
                ));
        when(getBibleVerseUseCase.getVerses(List.of(3L, 2L)))
                .thenReturn(List.of(
                        new BibleVerseResponse(3L, "GEN", 1, 3, "중립 예시 문구", null),
                        new BibleVerseResponse(2L, "EXO", 2, 1, "중립 예시 문구", null)
                ));

        NoteDetailResponse response = noteService.get(10L, 1L);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.verses()).hasSize(2);
        assertThat(response.verses().get(0).bibleVerseId()).isEqualTo(3L);
        assertThat(response.verses().get(1).bookCode()).isEqualTo("EXO");
        verify(getBibleVerseUseCase).getVerses(List.of(3L, 2L));
        verify(getBibleVerseUseCase, never()).getVerse(any());
    }

    @Test
    @DisplayName("get rejects missing, deleted, or other member note")
    void get_notFoundOrNotOwned_rejected() {
        when(noteRepository.findActiveByIdAndMemberId(1L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.get(20L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
    }

    @Test
    @DisplayName("listCategories returns category metadata")
    void listCategories_returnsMetadata() {
        NoteCategoryResponse response = noteService.listCategories();

        assertThat(response.categories()).hasSize(5);
        assertThat(response.categories())
                .extracting("category")
                .containsExactly(
                        NoteCategory.MEDITATION,
                        NoteCategory.SERMON,
                        NoteCategory.PRAYER,
                        NoteCategory.REPENTANCE,
                        NoteCategory.GRATITUDE
                );
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
    @DisplayName("draft lookup returns exists true with note detail when meditation draft exists")
    void getDraft_existing_returnsDetail() {
        Note note = persistedNote(1L, 10L, NoteCategory.MEDITATION, NoteStatus.DRAFT, 100L);
        when(noteRepository.findDraft(10L, NoteCategory.MEDITATION, 100L))
                .thenReturn(Optional.of(note));
        when(noteVerseRepository.findAllByNoteIdOrderByDisplayOrderAsc(1L))
                .thenReturn(List.of());

        NoteDraftResponse response = noteService.getDraft(10L, NoteCategory.MEDITATION, 100L);

        assertThat(response.exists()).isTrue();
        assertThat(response.note()).isNotNull();
        assertThat(response.note().id()).isEqualTo(1L);
        assertThat(response.note().category()).isEqualTo(NoteCategory.MEDITATION);
        assertThat(response.note().qtPassageId()).isEqualTo(100L);
        assertThat(response.note().status()).isEqualTo(NoteStatus.DRAFT);
        assertThat(response.note().verses()).isEmpty();
    }

    @Test
    @DisplayName("draft lookup only allows meditation category with qtPassageId")
    void getDraft_nonMeditationOrMissingQtPassage_rejected() {
        assertThatThrownBy(() -> noteService.getDraft(10L, NoteCategory.PRAYER, 100L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);

        assertThatThrownBy(() -> noteService.getDraft(10L, NoteCategory.MEDITATION, null))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("create meditation note requires qtPassageId")
    void create_meditationWithoutQtPassage_rejected() {
        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.MEDITATION, null, "묵상", "본문", null, null, null, null,
                List.of(), NoteStatus.DRAFT, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.create(10L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(noteQtClient, never()).validateReadable(any(), any());
    }

    @Test
    @DisplayName("create free note rejects qtPassageId")
    void create_freeNoteWithQtPassage_rejected() {
        CreateNoteCommand command = new CreateNoteCommand(
                NoteCategory.PRAYER, 100L, "기도", "본문", null, null, null, null,
                List.of(), NoteStatus.DRAFT, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.create(10L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
    }

    @Test
    @DisplayName("update saved prayer note replaces fields, status, and verses")
    void update_prayerNote_replacesFieldsStatusAndVerses() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.DRAFT, null);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(getBibleVerseUseCase.getVerses(List.of(3L, 2L)))
                .thenReturn(List.of(
                        new BibleVerseResponse(3L, "GEN", 1, 3, "중립 예시 문구", null),
                        new BibleVerseResponse(2L, "GEN", 1, 2, "중립 예시 문구", null)
                ));

        UpdateNoteCommand command = new UpdateNoteCommand(
                NoteCategory.PRAYER, null, "수정 제목", "수정 본문",
                "기억", "해석", "적용", "기도",
                List.of(3L, 3L, 2L), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        NoteSaveResponse response = noteService.update(10L, 1L, command);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo(NoteStatus.SAVED);
        assertThat(note.getTitle()).isEqualTo("수정 제목");
        assertThat(note.getBody()).isEqualTo("수정 본문");
        assertThat(note.getRememberSection()).isEqualTo("기억");
        assertThat(note.getStatus()).isEqualTo(NoteStatus.SAVED);
        assertThat(note.getSavedAt()).isNotNull();
        verify(noteVerseRepository).deleteByNoteId(1L);
        verify(noteVerseRepository).saveAll(noteVersesCaptor.capture());
        List<NoteVerse> savedVerses = StreamSupport.stream(noteVersesCaptor.getValue().spliterator(), false)
                .toList();
        assertThat(savedVerses)
                .extracting(NoteVerse::getBibleVerseId)
                .containsExactly(3L, 2L);
        assertThat(savedVerses)
                .extracting(NoteVerse::getDisplayOrder)
                .containsExactly((short) 1, (short) 2);
    }

    @Test
    @DisplayName("update saved note to draft clears savedAt")
    void update_savedNoteToDraft_clearsSavedAt() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.SAVED, null);
        assertThat(note.getSavedAt()).isNotNull();
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        UpdateNoteCommand command = new UpdateNoteCommand(
                NoteCategory.PRAYER, null, "기도", "본문", null, null, null, null,
                List.of(), NoteStatus.DRAFT, NoteVisibility.PRIVATE);

        NoteSaveResponse response = noteService.update(10L, 1L, command);

        assertThat(response.status()).isEqualTo(NoteStatus.DRAFT);
        assertThat(note.getStatus()).isEqualTo(NoteStatus.DRAFT);
        assertThat(note.getSavedAt()).isNull();
        verify(noteVerseRepository).deleteByNoteId(1L);
        verify(noteVerseRepository).saveAll(List.of());
    }

    @Test
    @DisplayName("update meditation note keeps active unique key")
    void update_meditationNote_keepsActiveUniqueKey() {
        Note note = persistedNote(1L, 10L, NoteCategory.MEDITATION, NoteStatus.DRAFT, 100L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        UpdateNoteCommand command = new UpdateNoteCommand(
                NoteCategory.MEDITATION, 100L, "묵상", "본문", null, null, null, null,
                List.of(), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        NoteSaveResponse response = noteService.update(10L, 1L, command);

        assertThat(response.status()).isEqualTo(NoteStatus.SAVED);
        assertThat(note.getActiveUniqueKey()).isEqualTo(Note.ACTIVE_KEY);
        assertThat(note.getQtPassageId()).isEqualTo(100L);
        verify(noteQtClient).validateReadable(10L, 100L);
        verify(noteRepository).existsByMemberIdAndQtPassageIdAndCategoryAndActiveUniqueKeyAndIdNot(
                10L, 100L, NoteCategory.MEDITATION, Note.ACTIVE_KEY, 1L);
    }

    @Test
    @DisplayName("update meditation to duplicate active QT note is rejected")
    void update_meditationDuplicate_rejected() {
        Note note = persistedNote(1L, 10L, NoteCategory.MEDITATION, NoteStatus.DRAFT, 100L);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(noteRepository.existsByMemberIdAndQtPassageIdAndCategoryAndActiveUniqueKeyAndIdNot(
                10L, 200L, NoteCategory.MEDITATION, Note.ACTIVE_KEY, 1L))
                .thenReturn(true);

        UpdateNoteCommand command = new UpdateNoteCommand(
                NoteCategory.MEDITATION, 200L, "묵상", "본문", null, null, null, null,
                List.of(), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.update(10L, 1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.DUPLICATE_NOTE);
        assertThat(note.getQtPassageId()).isEqualTo(100L);
        assertThat(note.getStatus()).isEqualTo(NoteStatus.DRAFT);
        verify(noteVerseRepository, never()).deleteByNoteId(any());
    }

    @Test
    @DisplayName("update with missing bible verse aborts verse replacement")
    void update_missingBibleVerse_rejected() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.DRAFT, null);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));
        when(getBibleVerseUseCase.getVerses(List.of(999L))).thenReturn(List.of());

        UpdateNoteCommand command = new UpdateNoteCommand(
                NoteCategory.PRAYER, null, "기도", "본문", null, null, null, null,
                List.of(999L), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.update(10L, 1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIBLE_VERSE_NOT_FOUND);
        verify(noteVerseRepository).deleteByNoteId(1L);
        verify(noteVerseRepository, never()).saveAll(any());
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
    @DisplayName("update rejects other member note")
    void update_otherMemberNote_rejected() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.SAVED, null);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        UpdateNoteCommand command = new UpdateNoteCommand(
                NoteCategory.PRAYER, null, "기도", "본문", null, null, null, null,
                List.of(), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.update(20L, 1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("update rejects missing note")
    void update_missingNote_rejected() {
        when(noteRepository.findById(1L)).thenReturn(Optional.empty());

        UpdateNoteCommand command = new UpdateNoteCommand(
                NoteCategory.PRAYER, null, "기도", "본문", null, null, null, null,
                List.of(), NoteStatus.SAVED, NoteVisibility.PRIVATE);

        assertThatThrownBy(() -> noteService.update(10L, 1L, command))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
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
        assertThat(note.getSavedAt()).isNull();
    }

    @Test
    @DisplayName("delete rejects missing note")
    void delete_missingNote_rejected() {
        when(noteRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService.delete(10L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
    }

    @Test
    @DisplayName("delete rejects other member note")
    void delete_otherMemberNote_rejected() {
        Note note = persistedNote(1L, 10L, NoteCategory.PRAYER, NoteStatus.SAVED, null);
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        assertThatThrownBy(() -> noteService.delete(20L, 1L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("delete is idempotent for already deleted note")
    void delete_alreadyDeletedNote_returnsWithoutException() {
        Note note = persistedNote(1L, 10L, NoteCategory.MEDITATION, NoteStatus.SAVED, 100L);
        note.delete(java.time.LocalDateTime.now());
        Object deletedAt = note.getDeletedAt();
        when(noteRepository.findById(1L)).thenReturn(Optional.of(note));

        assertThatNoException().isThrownBy(() -> noteService.delete(10L, 1L));
        assertThat(note.getDeletedAt()).isSameAs(deletedAt);
        assertThat(note.getStatus()).isEqualTo(NoteStatus.DELETED);
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
