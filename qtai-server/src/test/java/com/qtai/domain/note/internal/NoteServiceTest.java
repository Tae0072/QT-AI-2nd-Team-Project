package com.qtai.domain.note.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.NoteStatus;
import com.qtai.domain.note.api.dto.NoteListItem;
import com.qtai.domain.note.api.dto.NoteListResponse;

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
    private NoteService noteService;
    private Pageable defaultPageable;

    @BeforeEach
    void setUp() {
        noteRepository = mock(NoteRepository.class);
        noteService = new NoteService(noteRepository);
        defaultPageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "updatedAt"));
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
        when(note.getCreatedAt()).thenReturn(NOW);
        when(note.getUpdatedAt()).thenReturn(NOW);
        return note;
    }
}
