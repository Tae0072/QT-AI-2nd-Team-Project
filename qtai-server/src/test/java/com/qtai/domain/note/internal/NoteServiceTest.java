package com.qtai.domain.note.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.bible.api.GetBibleVerseUseCase;
import com.qtai.domain.bible.api.dto.BibleVerseResponse;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.dto.NoteCreateRequest;
import com.qtai.domain.note.api.dto.NoteListItem;
import com.qtai.domain.note.api.dto.NoteListResponse;
import com.qtai.domain.note.api.dto.NoteResponse;

/**
 * NoteService 단위 테스트.
 *
 * 검증 범위:
 * - Repository.search 호출 위임 (memberId 강제 필터 + nullable 선택 필터)
 * - Page&lt;Note&gt; → NoteListResponse 매핑 정확성
 * - placeholder 값 (visibility/qtDate/rangeLabel/shared) — 다음 PR 보강 예정
 * - Sort 객체 → "field,direction" 문자열 변환
 */
@SuppressWarnings("null") // Eclipse JDT strict null 분석에서 PageImpl(List, Pageable, long) 인자 추론 경고 억제. 빌드·실행 영향 없음.
class NoteServiceTest {

    private static final LocalDateTime NOW = LocalDateTime.parse("2026-05-26T10:30:00");

    private NoteRepository noteRepository;
    private NoteVerseRepository noteVerseRepository;
    private GetBibleVerseUseCase getBibleVerseUseCase;
    private NoteService noteService;
    private Pageable defaultPageable;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        noteVerseRepository = mock(NoteVerseRepository.class);
        getBibleVerseUseCase = mock(GetBibleVerseUseCase.class);
        noteService = new NoteService(noteRepository, noteVerseRepository, getBibleVerseUseCase);
        defaultPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }

    @Test
    @DisplayName("SERMON 생성 시 notes와 note_verses를 저장하고 구절 순서를 보존한다")
    void create_SERMON_정상생성_구절순서보존() {
        // given
        NoteCreateRequest request = new NoteCreateRequest(
                NoteCategory.SERMON,
                "주일 설교",
                "은혜를 기록",
                List.of(3L, 5L, 8L),
                NoteStatus.SAVED
        );
        stubExistingVerses(3L, 5L, 8L);
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            setField(note, "id", 99L);
            setField(note, "createdAt", NOW);
            setField(note, "updatedAt", NOW);
            return note;
        });
        when(noteVerseRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        NoteResponse response = noteService.create(10L, request);

        // then
        ArgumentCaptor<Note> noteCaptor = ArgumentCaptor.forClass(Note.class);
        verify(noteRepository).save(noteCaptor.capture());
        Note savedNote = noteCaptor.getValue();
        assertThat(savedNote.getMemberId()).isEqualTo(10L);
        assertThat(savedNote.getCategory()).isEqualTo(NoteCategory.SERMON);
        assertThat(savedNote.getStatus()).isEqualTo(NoteStatus.SAVED);
        assertThat(savedNote.getVisibility()).isEqualTo("PRIVATE");
        assertThat(savedNote.getQtPassageId()).isNull();
        assertThat(savedNote.getActiveUniqueKey()).isNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NoteVerse>> verseCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(noteVerseRepository).saveAll(verseCaptor.capture());
        List<NoteVerse> verses = toList(verseCaptor.getValue());
        assertThat(verses)
                .extracting(NoteVerse::getBibleVerseId)
                .containsExactly(3L, 5L, 8L);
        assertThat(verses)
                .extracting(NoteVerse::getDisplayOrder)
                .containsExactly((short) 1, (short) 2, (short) 3);
        assertThat(verses).allMatch(verse -> verse.getNoteId().equals(99L));

        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.category()).isEqualTo(NoteCategory.SERMON);
        assertThat(response.status()).isEqualTo(NoteStatus.SAVED);
        assertThat(response.visibility()).isEqualTo("PRIVATE");
        assertThat(response.title()).isEqualTo("주일 설교");
        assertThat(response.body()).isEqualTo("은혜를 기록");
        assertThat(response.verseIds()).containsExactly(3L, 5L, 8L);
        assertThat(response.createdAt()).isEqualTo(NOW);
        assertThat(response.savedAt()).isNotNull();
    }

    @Test
    @DisplayName("SERMON 생성 시 verseIds 중복은 첫 등장 순서만 저장한다")
    void create_SERMON_중복구절_첫등장순서만저장() {
        // given
        NoteCreateRequest request = new NoteCreateRequest(
                NoteCategory.SERMON,
                "설교",
                "본문",
                List.of(3L, 5L, 3L, 8L, 5L),
                null
        );
        stubExistingVerses(3L, 5L, 8L);
        when(noteRepository.save(any(Note.class))).thenAnswer(invocation -> {
            Note note = invocation.getArgument(0);
            setField(note, "id", 100L);
            return note;
        });
        when(noteVerseRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        NoteResponse response = noteService.create(10L, request);

        // then
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Iterable<NoteVerse>> verseCaptor = ArgumentCaptor.forClass(Iterable.class);
        verify(noteVerseRepository).saveAll(verseCaptor.capture());
        assertThat(toList(verseCaptor.getValue()))
                .extracting(NoteVerse::getBibleVerseId)
                .containsExactly(3L, 5L, 8L);
        assertThat(response.verseIds()).containsExactly(3L, 5L, 8L);
        assertThat(response.status()).isEqualTo(NoteStatus.SAVED);
    }

    @Test
    @DisplayName("제목과 본문이 모두 비어 있으면 INVALID_INPUT으로 실패하고 저장하지 않는다")
    void create_SERMON_제목본문없음_실패() {
        // given
        NoteCreateRequest request = new NoteCreateRequest(
                NoteCategory.SERMON,
                " ",
                "",
                List.of(3L),
                NoteStatus.SAVED
        );

        // when & then
        assertThatThrownBy(() -> noteService.create(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(noteRepository, never()).save(any());
        verify(noteVerseRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("verseIds가 비어 있으면 INVALID_INPUT으로 실패하고 저장하지 않는다")
    void create_SERMON_구절없음_실패() {
        // given
        NoteCreateRequest request = new NoteCreateRequest(
                NoteCategory.SERMON,
                "설교",
                "본문",
                List.of(),
                NoteStatus.SAVED
        );

        // when & then
        assertThatThrownBy(() -> noteService.create(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_INPUT);
        verify(noteRepository, never()).save(any());
        verify(noteVerseRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("존재하지 않는 bibleVerseId가 있으면 notes와 note_verses를 저장하지 않는다")
    void create_SERMON_존재하지않는구절_저장안함() {
        // given
        NoteCreateRequest request = new NoteCreateRequest(
                NoteCategory.SERMON,
                "설교",
                "본문",
                List.of(3L, 999L),
                NoteStatus.SAVED
        );
        when(getBibleVerseUseCase.getVerse(3L)).thenReturn(verse(3L));
        when(getBibleVerseUseCase.getVerse(999L))
                .thenThrow(new BusinessException(ErrorCode.BIBLE_VERSE_NOT_FOUND));

        // when & then
        assertThatThrownBy(() -> noteService.create(10L, request))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.BIBLE_VERSE_NOT_FOUND);
        verify(noteRepository, never()).save(any());
        verify(noteVerseRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("Repository.search 결과를 NoteListResponse로 매핑하고 페이지 메타를 채운다")
    void list_정상조회_매핑검증() {
        // given
        Long memberId = 10L;
        Note note = mockNote(1L, NoteCategory.PRAYER, "기도제목 1", NoteStatus.SAVED);
        Page<Note> stub = new PageImpl<>(List.of(note), defaultPageable, 1L);
        when(noteRepository.search(eq(memberId), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(stub);

        // when
        NoteListResponse response = noteService.list(memberId, null, null, null, defaultPageable);

        // then — 페이지 메타
        assertThat(response.content()).hasSize(1);
        assertThat(response.page()).isEqualTo(0);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(1L);
        assertThat(response.totalPages()).isEqualTo(1);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
        assertThat(response.sort()).isEqualTo("updatedAt,desc");

        // then — 실제 필드 매핑
        NoteListItem item = response.content().get(0);
        assertThat(item.id()).isEqualTo(1L);
        assertThat(item.category()).isEqualTo(NoteCategory.PRAYER);
        assertThat(item.title()).isEqualTo("기도제목 1");
        assertThat(item.status()).isEqualTo(NoteStatus.SAVED);
        assertThat(item.createdAt()).isEqualTo(NOW);
        assertThat(item.updatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("placeholder 필드: visibility=PRIVATE, qtDate/rangeLabel=null, shared=false")
    void list_placeholder값_검증() {
        // given
        Note note = mockNote(2L, NoteCategory.MEDITATION, "묵상", NoteStatus.SAVED);
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(note), defaultPageable, 1L));

        // when
        NoteListResponse response = noteService.list(10L, null, null, null, defaultPageable);

        // then
        NoteListItem item = response.content().get(0);
        assertThat(item.visibility()).isEqualTo("PRIVATE");
        assertThat(item.qtDate()).isNull();
        assertThat(item.rangeLabel()).isNull();
        assertThat(item.shared()).isFalse();
    }

    @Test
    @DisplayName("빈 결과도 정상 매핑 (content 비어있음, totalElements=0)")
    void list_빈결과_처리() {
        // given
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), defaultPageable, 0L));

        // when
        NoteListResponse response = noteService.list(10L, null, null, null, defaultPageable);

        // then
        assertThat(response.content()).isEmpty();
        assertThat(response.totalElements()).isEqualTo(0L);
        assertThat(response.totalPages()).isEqualTo(0);
        assertThat(response.first()).isTrue();
        assertThat(response.last()).isTrue();
    }

    @Test
    @DisplayName("필터(category, status, q)를 Repository에 그대로 전달")
    void list_필터전달_검증() {
        // given
        ArgumentCaptor<Long> memberIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<NoteCategory> categoryCaptor = ArgumentCaptor.forClass(NoteCategory.class);
        ArgumentCaptor<NoteStatus> statusCaptor = ArgumentCaptor.forClass(NoteStatus.class);
        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), defaultPageable, 0L));

        // when
        noteService.list(10L, NoteCategory.PRAYER, NoteStatus.SAVED, "기도", defaultPageable);

        // then — 캡처된 인자들이 호출 시 전달된 값과 일치
        verify(noteRepository).search(
                memberIdCaptor.capture(),
                categoryCaptor.capture(),
                statusCaptor.capture(),
                qCaptor.capture(),
                any(Pageable.class)
        );
        assertThat(memberIdCaptor.getValue()).isEqualTo(10L);
        assertThat(categoryCaptor.getValue()).isEqualTo(NoteCategory.PRAYER);
        assertThat(statusCaptor.getValue()).isEqualTo(NoteStatus.SAVED);
        assertThat(qCaptor.getValue()).isEqualTo("기도");
    }

    @Test
    @DisplayName("q가 null이면 Repository에도 null이 그대로 전달된다")
    void list_q가_null이면_Repository에도_null_전달() {
        // given
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), defaultPageable, 0L));

        // when
        noteService.list(10L, null, null, null, defaultPageable);

        // then
        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
        verify(noteRepository).search(any(), any(), any(), qCaptor.capture(), any(Pageable.class));
        assertThat(qCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("q가 빈 문자열이면 Repository에 null이 전달된다 (LIKE '%%' 전체 매치 사고 방지)")
    void list_q가_빈문자열이면_Repository에_null_전달() {
        // given
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), defaultPageable, 0L));

        // when
        noteService.list(10L, null, null, "", defaultPageable);

        // then
        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
        verify(noteRepository).search(any(), any(), any(), qCaptor.capture(), any(Pageable.class));
        assertThat(qCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("q가 공백만 있으면 Repository에 null이 전달된다 (isBlank 가드)")
    void list_q가_공백만이면_Repository에_null_전달() {
        // given
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), defaultPageable, 0L));

        // when
        noteService.list(10L, null, null, "   ", defaultPageable);

        // then
        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
        verify(noteRepository).search(any(), any(), any(), qCaptor.capture(), any(Pageable.class));
        assertThat(qCaptor.getValue()).isNull();
    }

    @Test
    @DisplayName("q에 LIKE 와일드카드(%, _, \\)가 포함되면 이스케이프된 값으로 Repository에 전달된다")
    void list_q_와일드카드_이스케이프() {
        // given
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), defaultPageable, 0L));

        // when — % 단일 케이스
        noteService.list(10L, null, null, "50%할인", defaultPageable);

        // then
        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
        verify(noteRepository).search(any(), any(), any(), qCaptor.capture(), any(Pageable.class));
        assertThat(qCaptor.getValue()).isEqualTo("50\\%할인");
    }

    @Test
    @DisplayName("q에 언더스코어(_)가 포함되면 이스케이프된 값(\\_)으로 전달된다")
    void list_q_언더스코어_이스케이프() {
        // given
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), defaultPageable, 0L));

        // when
        noteService.list(10L, null, null, "이름_검색", defaultPageable);

        // then
        ArgumentCaptor<String> qCaptor = ArgumentCaptor.forClass(String.class);
        verify(noteRepository).search(any(), any(), any(), qCaptor.capture(), any(Pageable.class));
        assertThat(qCaptor.getValue()).isEqualTo("이름\\_검색");
    }

    @Test
    @DisplayName("Sort.unsorted()면 default 'updatedAt,desc'로 응답한다")
    void list_정렬없을때_default값() {
        // given
        Pageable unsorted = PageRequest.of(0, 20, Sort.unsorted());
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), unsorted, 0L));

        // when
        NoteListResponse response = noteService.list(10L, null, null, null, unsorted);

        // then
        assertThat(response.sort()).isEqualTo("updatedAt,desc");
    }

    @Test
    @DisplayName("Sort에 여러 필드가 있어도 첫 번째 정렬만 응답 sort 문자열로 사용")
    void list_정렬다중필드_첫번째만() {
        // given
        Pageable multiSort = PageRequest.of(0, 20,
                Sort.by(Sort.Direction.ASC, "createdAt")
                        .and(Sort.by(Sort.Direction.DESC, "id")));
        when(noteRepository.search(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), multiSort, 0L));

        // when
        NoteListResponse response = noteService.list(10L, null, null, null, multiSort);

        // then
        assertThat(response.sort()).isEqualTo("createdAt,asc");
    }

    /**
     * Note는 BaseEntity 필드(id/createdAt/updatedAt)가 JPA Auditing으로 영속화 시점에
     * 채워지기 때문에, 단위 테스트에선 mock으로 getter 응답을 직접 제어한다.
     */
    private static Note mockNote(Long id, NoteCategory category, String title, NoteStatus status) {
        Note note = mock(Note.class);
        when(note.getId()).thenReturn(id);
        when(note.getCategory()).thenReturn(category);
        when(note.getTitle()).thenReturn(title);
        when(note.getStatus()).thenReturn(status);
        when(note.getVisibility()).thenReturn("PRIVATE");
        when(note.getCreatedAt()).thenReturn(NOW);
        when(note.getUpdatedAt()).thenReturn(NOW);
        return note;
    }

    private void stubExistingVerses(Long... verseIds) {
        for (Long verseId : verseIds) {
            when(getBibleVerseUseCase.getVerse(verseId)).thenReturn(verse(verseId));
        }
    }

    private static BibleVerseResponse verse(Long verseId) {
        return new BibleVerseResponse(verseId, "GEN", 1, verseId.intValue(), "본문", "text");
    }

    private static List<NoteVerse> toList(Iterable<NoteVerse> verses) {
        List<NoteVerse> result = new ArrayList<>();
        verses.forEach(result::add);
        return result;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = findField(target.getClass(), fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to set field: " + fieldName, e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        throw new NoSuchFieldException(fieldName);
    }
}
