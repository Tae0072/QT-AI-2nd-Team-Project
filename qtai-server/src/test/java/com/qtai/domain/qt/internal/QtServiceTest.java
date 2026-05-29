package com.qtai.domain.qt.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.note.api.GetNoteUseCase;
import com.qtai.domain.note.api.NoteCategory;
import com.qtai.domain.note.api.dto.NoteDetailResponse;
import com.qtai.domain.note.api.dto.NoteDraftResponse;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.qt.api.dto.TodayQtResponse;

/**
 * QtService 단위 테스트 — Note 도메인 연동 및 draftNoteId enrich 검증.
 *
 * <p>본문 조회 로직(HIT/STALE_FALLBACK/MISS/EMPTY)은
 * {@link QtPassageLookupTest}에서 검증한다.
 * 이 테스트는 QtService의 오케스트레이션 역할을 검증한다:
 * <ul>
 *   <li>QtPassageLookup에서 공용 캐시 데이터 위임</li>
 *   <li>note 도메인(GetNoteUseCase)에서 draftNoteId 조회 후 enrich</li>
 *   <li>예외 상황에서 방어적 처리 (draftNoteId=null fallback)</li>
 * </ul>
 */
class QtServiceTest {

    private QtPassageLookup passageLookup;
    private QtPassageRepository qtPassageRepository;
    private QtPassageVerseRepository qtPassageVerseRepository;
    private GetNoteUseCase getNoteUseCase;
    private QtService qtService;

    @BeforeEach
    void setUp() {
        passageLookup = Mockito.mock(QtPassageLookup.class);
        qtPassageRepository = Mockito.mock(QtPassageRepository.class);
        qtPassageVerseRepository = Mockito.mock(QtPassageVerseRepository.class);
        getNoteUseCase = Mockito.mock(GetNoteUseCase.class);
        qtService = new QtService(passageLookup, qtPassageRepository, qtPassageVerseRepository, getNoteUseCase);
    }

    /**
     * note 도메인의 NoteDetailResponse 더미를 생성한다.
     * id 필드만 필요하므로 나머지는 null/기본값.
     */
    private static NoteDetailResponse dummyNoteDetail(Long noteId) {
        return new NoteDetailResponse(
                noteId,      // id
                100L,        // memberId
                NoteCategory.MEDITATION,  // category
                1L,          // qtPassageId
                "묵상 제목",  // title
                "본문",       // body
                null, null, null, null,  // remember/interpret/apply/pray sections
                null,        // status
                null,        // visibility
                null,        // qtDate
                null,        // rangeLabel
                false,       // shared
                null,        // savedAt
                null,        // createdAt
                null,        // updatedAt
                java.util.List.of() // verses
        );
    }

    // ------------------------------------------------------------------
    // getToday() 테스트
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getToday — Note 도메인 연동")
    class GetTodayTest {

        @Test
        @DisplayName("DRAFT 노트가 있으면 draftNoteId를 enrich하여 반환")
        void DRAFT_노트_있으면_draftNoteId_포함() {
            // given: passageLookup이 HIT 본문 반환
            TodayQtResponse base = new TodayQtResponse(
                    1L, "2026-05-28", "하나님이 세상을 이처럼 사랑하사",
                    "MISSING", false, null, "HIT");
            when(passageLookup.findTodayPassage()).thenReturn(base);

            // given: note 도메인에서 DRAFT 노트 존재
            NoteDraftResponse draft = new NoteDraftResponse(true, dummyNoteDetail(42L));
            when(getNoteUseCase.getDraft(100L, NoteCategory.MEDITATION, 1L)).thenReturn(draft);

            // when
            TodayQtResponse response = qtService.getToday(100L);

            // then: draftNoteId가 enrich됨
            assertThat(response.draftNoteId()).isEqualTo(42L);
            assertThat(response.qtPassageId()).isEqualTo(1L);
            assertThat(response.cacheStatus()).isEqualTo("HIT");
            assertThat(response.title()).isEqualTo("하나님이 세상을 이처럼 사랑하사");
        }

        @Test
        @DisplayName("DRAFT 노트가 없으면 draftNoteId=null 반환")
        void DRAFT_노트_없으면_draftNoteId_null() {
            // given
            TodayQtResponse base = new TodayQtResponse(
                    1L, "2026-05-28", "테스트 본문",
                    "MISSING", false, null, "HIT");
            when(passageLookup.findTodayPassage()).thenReturn(base);

            NoteDraftResponse noDraft = new NoteDraftResponse(false, null);
            when(getNoteUseCase.getDraft(100L, NoteCategory.MEDITATION, 1L)).thenReturn(noDraft);

            // when
            TodayQtResponse response = qtService.getToday(100L);

            // then
            assertThat(response.draftNoteId()).isNull();
            assertThat(response.qtPassageId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("memberId가 null이면 note 도메인 호출 생략, draftNoteId=null")
        void memberId_null이면_note_조회_생략() {
            // given
            TodayQtResponse base = new TodayQtResponse(
                    1L, "2026-05-28", "테스트 본문",
                    "MISSING", false, null, "HIT");
            when(passageLookup.findTodayPassage()).thenReturn(base);

            // when
            TodayQtResponse response = qtService.getToday(null);

            // then
            assertThat(response.draftNoteId()).isNull();
            verify(getNoteUseCase, never()).getDraft(anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("qtPassageId가 null이면(EMPTY/MISS) note 도메인 호출 생략")
        void qtPassageId_null이면_note_조회_생략() {
            // given: EMPTY 응답 (qtPassageId=null)
            TodayQtResponse base = new TodayQtResponse(
                    null, null, null, "DISABLED", false, null, "EMPTY");
            when(passageLookup.findTodayPassage()).thenReturn(base);

            // when
            TodayQtResponse response = qtService.getToday(100L);

            // then
            assertThat(response.draftNoteId()).isNull();
            verify(getNoteUseCase, never()).getDraft(anyLong(), any(), anyLong());
        }

        @Test
        @DisplayName("note 도메인 호출 실패 시 draftNoteId=null로 방어적 처리")
        void note_도메인_예외_시_방어적_처리() {
            // given
            TodayQtResponse base = new TodayQtResponse(
                    1L, "2026-05-28", "테스트 본문",
                    "MISSING", false, null, "HIT");
            when(passageLookup.findTodayPassage()).thenReturn(base);

            when(getNoteUseCase.getDraft(100L, NoteCategory.MEDITATION, 1L))
                    .thenThrow(new RuntimeException("DB connection error"));

            // when: 예외가 전파되지 않고 draftNoteId=null로 fallback
            TodayQtResponse response = qtService.getToday(100L);

            // then
            assertThat(response.draftNoteId()).isNull();
            assertThat(response.qtPassageId()).isEqualTo(1L);
            assertThat(response.cacheStatus()).isEqualTo("HIT");
        }

        @Test
        @DisplayName("STALE_FALLBACK 응답에도 draftNoteId enrich 정상 동작")
        void STALE_FALLBACK에도_draftNoteId_enrich() {
            // given: 새벽 구간, 어제 본문 STALE_FALLBACK
            TodayQtResponse base = new TodayQtResponse(
                    2L, "2026-05-27", "여호와는 나의 목자시니",
                    "MISSING", false, null, "STALE_FALLBACK");
            when(passageLookup.findTodayPassage()).thenReturn(base);

            NoteDraftResponse draft = new NoteDraftResponse(true, dummyNoteDetail(99L));
            when(getNoteUseCase.getDraft(100L, NoteCategory.MEDITATION, 2L)).thenReturn(draft);

            // when
            TodayQtResponse response = qtService.getToday(100L);

            // then
            assertThat(response.draftNoteId()).isEqualTo(99L);
            assertThat(response.cacheStatus()).isEqualTo("STALE_FALLBACK");
        }
    }

    // ------------------------------------------------------------------
    // getPassage() 테스트
    // ------------------------------------------------------------------

    @Nested
    @DisplayName("getPassage — 특정 QT 본문 조회 + Note 연동")
    class GetPassageTest {

        @Test
        @DisplayName("존재하는 본문 + DRAFT 노트가 있으면 draftNoteId 포함")
        void 본문_존재_DRAFT_있음() {
            // given
            QtPassage passage = QtPassageFixture.createPassage(5L,
                    LocalDate.of(2026, 5, 26), "태초에 하나님이");
            when(qtPassageRepository.findById(5L)).thenReturn(Optional.of(passage));

            NoteDraftResponse draft = new NoteDraftResponse(true, dummyNoteDetail(77L));
            when(getNoteUseCase.getDraft(100L, NoteCategory.MEDITATION, 5L)).thenReturn(draft);

            // when
            TodayQtResponse response = qtService.getPassage(100L, 5L);

            // then
            assertThat(response.qtPassageId()).isEqualTo(5L);
            assertThat(response.draftNoteId()).isEqualTo(77L);
            assertThat(response.cacheStatus()).isEqualTo("HIT");
        }

        @Test
        @DisplayName("존재하는 본문 + DRAFT 노트가 없으면 draftNoteId=null")
        void 본문_존재_DRAFT_없음() {
            // given
            QtPassage passage = QtPassageFixture.createPassage(5L,
                    LocalDate.of(2026, 5, 26), "태초에 하나님이");
            when(qtPassageRepository.findById(5L)).thenReturn(Optional.of(passage));

            NoteDraftResponse noDraft = new NoteDraftResponse(false, null);
            when(getNoteUseCase.getDraft(100L, NoteCategory.MEDITATION, 5L)).thenReturn(noDraft);

            // when
            TodayQtResponse response = qtService.getPassage(100L, 5L);

            // then
            assertThat(response.qtPassageId()).isEqualTo(5L);
            assertThat(response.draftNoteId()).isNull();
        }

        @Test
        @DisplayName("존재하지 않는 본문 ID로 조회하면 QT_PASSAGE_NOT_FOUND")
        void 존재하지_않는_본문_조회_실패() {
            // given
            when(qtPassageRepository.findById(999L)).thenReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> qtService.getPassage(100L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getErrorCode()).isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND);
                    });
        }

        @Test
        @DisplayName("note 도메인 실패 시에도 본문은 정상 반환 (draftNoteId=null)")
        void note_실패_시_본문_정상_반환() {
            // given
            QtPassage passage = QtPassageFixture.createPassage(5L,
                    LocalDate.of(2026, 5, 26), "태초에 하나님이");
            when(qtPassageRepository.findById(5L)).thenReturn(Optional.of(passage));

            when(getNoteUseCase.getDraft(eq(100L), any(), eq(5L)))
                    .thenThrow(new RuntimeException("note service down"));

            // when
            TodayQtResponse response = qtService.getPassage(100L, 5L);

            // then
            assertThat(response.qtPassageId()).isEqualTo(5L);
            assertThat(response.draftNoteId()).isNull();
        }
    }

    @Nested
    @DisplayName("getContentContext — Study 도메인 공개 context")
    class GetContentContextTest {

        @Test
        @DisplayName("QT 본문과 연결된 verse id를 displayOrder 순서로 반환")
        void 본문_context_조회_성공() {
            QtPassage passage = QtPassageFixture.createPassage(5L,
                    LocalDate.of(2026, 5, 26), "태초에 하나님이");
            when(qtPassageRepository.findById(5L)).thenReturn(Optional.of(passage));
            when(qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(5L))
                    .thenReturn(List.of(
                            verse(5L, 100L, (short) 1),
                            verse(5L, 101L, (short) 2)
                    ));

            QtPassageContentContext context = qtService.getContentContext(5L);

            assertThat(context.qtPassageId()).isEqualTo(5L);
            assertThat(context.qtDate()).isEqualTo(LocalDate.of(2026, 5, 26));
            assertThat(context.title()).isEqualTo("태초에 하나님이");
            assertThat(context.verseIds()).containsExactly(100L, 101L);
            assertThat(context.published()).isTrue();
        }

        @Test
        @DisplayName("qtPassageId가 1보다 작으면 INVALID_INPUT")
        void 본문_context_잘못된_id() {
            assertThatThrownBy(() -> qtService.getContentContext(0L))
                    .isInstanceOfSatisfying(BusinessException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("존재하지 않는 본문이면 QT_PASSAGE_NOT_FOUND")
        void 본문_context_미존재() {
            when(qtPassageRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> qtService.getContentContext(999L))
                    .isInstanceOfSatisfying(BusinessException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND));
        }

        private QtPassageVerse verse(Long qtPassageId, Long bibleVerseId, Short displayOrder) {
            try {
                QtPassageVerse verse = QtPassageVerse.class.getDeclaredConstructor().newInstance();
                setField(verse, "qtPassageId", qtPassageId);
                setField(verse, "bibleVerseId", bibleVerseId);
                setField(verse, "displayOrder", displayOrder);
                return verse;
            } catch (Exception e) {
                throw new RuntimeException("테스트 QtPassageVerse 생성 실패", e);
            }
        }

        private void setField(Object target, String fieldName, Object value) throws Exception {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
    }
}
