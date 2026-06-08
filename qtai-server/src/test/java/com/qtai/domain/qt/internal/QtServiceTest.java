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
    private TodayQtRangeResolver rangeResolver;
    private GetNoteUseCase getNoteUseCase;
    private com.qtai.domain.study.api.GetQtStudyAvailabilityUseCase getQtStudyAvailabilityUseCase;
    private QtService qtService;

    /** 공개 게이트 기준 '오늘' — 2026-06-01 (KST). */
    private static final java.time.Clock FIXED_CLOCK = java.time.Clock.fixed(
            java.time.Instant.parse("2026-06-01T03:00:00Z"),
            java.time.ZoneId.of("Asia/Seoul")
    );

    @BeforeEach
    void setUp() {
        passageLookup = Mockito.mock(QtPassageLookup.class);
        qtPassageRepository = Mockito.mock(QtPassageRepository.class);
        qtPassageVerseRepository = Mockito.mock(QtPassageVerseRepository.class);
        rangeResolver = Mockito.mock(TodayQtRangeResolver.class);
        getNoteUseCase = Mockito.mock(GetNoteUseCase.class);
        getQtStudyAvailabilityUseCase =
                Mockito.mock(com.qtai.domain.study.api.GetQtStudyAvailabilityUseCase.class);
        // 기본 가용성: 콘텐츠 없음 — 개별 테스트에서 필요 시 override
        when(getQtStudyAvailabilityUseCase.getAvailability(any(), any()))
                .thenReturn(new com.qtai.domain.study.api.dto.QtStudyAvailability("MISSING", false));
        qtService = new QtService(
                passageLookup,
                qtPassageRepository,
                qtPassageVerseRepository,
                rangeResolver,
                getNoteUseCase,
                getQtStudyAvailabilityUseCase,
                FIXED_CLOCK
        );
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

        @Test
        @DisplayName("공개 게이트(§6) — 선등록된 미래 본문은 id를 알아도 404 (00:00 공개 전 비노출)")
        void 미래_본문_조회는_404() {
            // given — 오늘(고정 Clock)은 2026-06-01, 본문은 내일(06-02) 선등록분
            QtPassage future = QtPassageFixture.createPassage(8L,
                    LocalDate.of(2026, 6, 2), "내일 본문");
            when(qtPassageRepository.findById(8L)).thenReturn(Optional.of(future));

            // when & then — 존재 은닉을 위해 NOT_FOUND로 응답, note 조회도 발생하지 않음
            assertThatThrownBy(() -> qtService.getPassage(100L, 8L))
                    .isInstanceOfSatisfying(BusinessException.class, ex ->
                            assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND));
            verify(getNoteUseCase, never()).getDraft(any(), any(), any());
        }

        @Test
        @DisplayName("공개 게이트 — 오늘 날짜 본문은 00:00부터 id 조회 가능")
        void 오늘_본문_조회는_허용() {
            QtPassage today = QtPassageFixture.createPassage(7L,
                    LocalDate.of(2026, 6, 1), "오늘 본문");
            when(qtPassageRepository.findById(7L)).thenReturn(Optional.of(today));

            TodayQtResponse response = qtService.getPassage(null, 7L);

            assertThat(response.qtPassageId()).isEqualTo(7L);
        }

        @Test
        @DisplayName("Today QT 100%(§6) — 승인 클립·해설이 있으면 READY/true로 enrich (하드코딩 회귀 방지)")
        void study_가용성_enrich_성공() {
            QtPassage passage = QtPassageFixture.createPassage(5L,
                    LocalDate.of(2026, 5, 26), "태초에 하나님이");
            when(qtPassageRepository.findById(5L)).thenReturn(Optional.of(passage));
            when(getQtStudyAvailabilityUseCase.getAvailability(eq(5L), any()))
                    .thenReturn(new com.qtai.domain.study.api.dto.QtStudyAvailability("READY", true));

            TodayQtResponse response = qtService.getPassage(100L, 5L);

            assertThat(response.simulatorStatus()).isEqualTo("READY");
            assertThat(response.hasExplanation()).isTrue();
        }

        @Test
        @DisplayName("study 가용성 조회 실패 시 응답은 기본값(MISSING/false)으로 유지된다")
        void study_가용성_실패_fallback() {
            QtPassage passage = QtPassageFixture.createPassage(5L,
                    LocalDate.of(2026, 5, 26), "태초에 하나님이");
            when(qtPassageRepository.findById(5L)).thenReturn(Optional.of(passage));
            when(getQtStudyAvailabilityUseCase.getAvailability(any(), any()))
                    .thenThrow(new RuntimeException("study service down"));

            TodayQtResponse response = qtService.getPassage(100L, 5L);

            assertThat(response.qtPassageId()).isEqualTo(5L);
            assertThat(response.simulatorStatus()).isEqualTo("MISSING");
            assertThat(response.hasExplanation()).isFalse();
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

        @Test
        @DisplayName("findContentContextByDate — 노출 정책(STALE_FALLBACK)·캐시 없이 날짜 직접 조회 (내부 배치용)")
        void 날짜_기반_context_조회_성공() {
            QtPassage passage = QtPassageFixture.createPassage(5L,
                    LocalDate.of(2026, 6, 1), "오늘 본문");
            when(qtPassageRepository.findByQtDate(LocalDate.of(2026, 6, 1)))
                    .thenReturn(Optional.of(passage));
            when(qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(5L))
                    .thenReturn(List.of(verse(5L, 100L, (short) 1)));

            Optional<QtPassageContentContext> context =
                    qtService.findContentContextByDate(LocalDate.of(2026, 6, 1));

            assertThat(context).isPresent();
            assertThat(context.get().qtPassageId()).isEqualTo(5L);
            assertThat(context.get().verseIds()).containsExactly(100L);
        }

        @Test
        @DisplayName("findContentContextByDate — 해당 날짜 본문이 없으면 empty (예외 아님: 배치 사전조건 판단용)")
        void 날짜_기반_context_미존재() {
            when(qtPassageRepository.findByQtDate(LocalDate.of(2026, 6, 2)))
                    .thenReturn(Optional.empty());

            assertThat(qtService.findContentContextByDate(LocalDate.of(2026, 6, 2))).isEmpty();
        }

        @Test
        @DisplayName("findContentContextByDate — null 날짜는 INVALID_INPUT")
        void 날짜_기반_context_null_날짜() {
            assertThatThrownBy(() -> qtService.findContentContextByDate(null))
                    .isInstanceOfSatisfying(BusinessException.class, exception ->
                            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("공개 게이트 — 미래 날짜 본문의 context는 published=false (study 노출 차단)")
        void 미래_본문_context는_published_false() {
            QtPassage future = QtPassageFixture.createPassage(9L,
                    LocalDate.of(2026, 6, 2), "내일 본문");
            when(qtPassageRepository.findById(9L)).thenReturn(Optional.of(future));
            when(qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(9L))
                    .thenReturn(List.of());

            QtPassageContentContext context = qtService.getContentContext(9L);

            assertThat(context.published()).isFalse();
        }

        @Test
        @DisplayName("공개 게이트 — 오늘 날짜 본문의 context는 published=true")
        void 오늘_본문_context는_published_true() {
            QtPassage today = QtPassageFixture.createPassage(10L,
                    LocalDate.of(2026, 6, 1), "오늘 본문");
            when(qtPassageRepository.findById(10L)).thenReturn(Optional.of(today));
            when(qtPassageVerseRepository.findByQtPassageIdOrderByDisplayOrderAsc(10L))
                    .thenReturn(List.of());

            assertThat(qtService.getContentContext(10L).published()).isTrue();
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
