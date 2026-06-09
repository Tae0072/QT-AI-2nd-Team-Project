package com.qtai.domain.note.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.dto.CreateNoteCommand;
import com.qtai.domain.note.client.qt.NoteQtClient;
import com.qtai.domain.sharing.api.MarkSourceNoteDeletedUseCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * 노트 서비스 검증 로직 단위 테스트.
 *
 * <p>저장 전 입력 검증(카테고리별 qtPassageId/구절 요구사항)과 소유권 검증을 다룬다.
 * 검증이 먼저 실패하는 경로라 외부 포트(bible/qt)는 거의 호출되지 않는다.
 */
@ExtendWith(MockitoExtension.class)
class NoteServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-10T00:00:00Z"), ZoneId.of("Asia/Seoul"));

    @Mock
    private NoteRepository noteRepository;
    @Mock
    private NoteVerseRepository noteVerseRepository;
    @Mock
    private GetBibleVerseUseCase getBibleVerseUseCase;
    @Mock
    private NoteQtClient noteQtClient;
    @Mock
    private MarkSourceNoteDeletedUseCase markSourceNoteDeletedUseCase;
    @Mock
    private JournalEventOutbox journalEventOutbox;

    private NoteService noteService() {
        return new NoteService(noteRepository, noteVerseRepository, getBibleVerseUseCase,
                noteQtClient, markSourceNoteDeletedUseCase, journalEventOutbox, CLOCK);
    }

    private static CreateNoteCommand command(NoteCategory category, Long qtPassageId, List<Long> verseIds) {
        return new CreateNoteCommand(category, qtPassageId, "제목", "본문",
                null, null, null, null, verseIds, null, null);
    }

    @Test
    void 묵상노트_생성에_qtPassageId가_없으면_NOTE_QT_PASSAGE_REQUIRED() {
        assertThatThrownBy(() -> noteService().create(1L, command(NoteCategory.MEDITATION, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_QT_PASSAGE_REQUIRED);
    }

    @Test
    void 묵상노트가_아닌데_qtPassageId가_있으면_NOTE_QT_PASSAGE_FORBIDDEN() {
        assertThatThrownBy(() -> noteService().create(1L, command(NoteCategory.SERMON, 5L, List.of(1L))))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_QT_PASSAGE_FORBIDDEN);
    }

    @Test
    void 설교노트에_구절이_없으면_NOTE_VERSE_REQUIRED() {
        assertThatThrownBy(() -> noteService().create(1L, command(NoteCategory.SERMON, null, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_VERSE_REQUIRED);
    }

    @Test
    void 없는_노트_조회면_NOTE_NOT_FOUND() {
        when(noteRepository.findActiveByIdAndMemberId(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> noteService().get(1L, 99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTE_NOT_FOUND);
    }
}
