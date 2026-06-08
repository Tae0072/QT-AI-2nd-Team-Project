package com.qtai.domain.study.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import com.qtai.domain.study.api.dto.QtStudyContentResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static com.qtai.support.TestEntityFactory.glossaryTerm;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QtStudyContentServiceTest {

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase =
            mock(GetQtPassageContentContextUseCase.class);
    private final VerseExplanationService verseExplanationService = mock(VerseExplanationService.class);
    private final GlossaryTermRepository glossaryTermRepository = mock(GlossaryTermRepository.class);
    private final QtStudyContentService service = new QtStudyContentService(
            getQtPassageContentContextUseCase,
            verseExplanationService,
            glossaryTermRepository
    );

    @Test
    @DisplayName("승인 활성 해설과 승인 용어만 QT 절 순서로 조립한다")
    void getStudyContent_returnsApprovedContentInVerseOrder() {
        when(getQtPassageContentContextUseCase.getContentContext(10L))
                .thenReturn(context(List.of(2L, 1L)));
        when(verseExplanationService.listApprovedByVerseIds(List.of(2L, 1L)))
                .thenReturn(List.of(
                        new ApprovedVerseExplanationResponse(1L, "summary-1", "explanation-1", "source-1", 101L),
                        new ApprovedVerseExplanationResponse(2L, "summary-2", "explanation-2", "source-2", 102L)
                ));
        when(glossaryTermRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKeyOrderByBibleVerseIdAscIdAsc(
                List.of(2L, 1L),
                GlossaryTermStatus.APPROVED,
                GlossaryTerm.ACTIVE_UNIQUE_KEY
        )).thenReturn(List.of(
                glossaryTerm(11L, 1L, GlossaryTermStatus.APPROVED, "term-1"),
                glossaryTerm(12L, 2L, GlossaryTermStatus.APPROVED, "term-2")
        ));

        QtStudyContentResponse response = service.getStudyContent(10L);

        assertThat(response.summary()).isEqualTo("summary-2 summary-1");
        assertThat(response.explanations()).extracting(QtStudyContentResponse.ExplanationItem::verseId)
                .containsExactly(2L, 1L);
        assertThat(response.glossaryTerms()).extracting(QtStudyContentResponse.GlossaryTermItem::verseId)
                .containsExactly(2L, 1L);
        assertThat(response.glossaryTerms()).extracting(QtStudyContentResponse.GlossaryTermItem::term)
                .containsExactly("term-2", "term-1");
    }

    @Test
    @DisplayName("승인 해설 summary가 없으면 summary는 null이다")
    void getStudyContent_whenNoSummary_returnsNullSummary() {
        when(getQtPassageContentContextUseCase.getContentContext(10L))
                .thenReturn(context(List.of(1L)));
        when(verseExplanationService.listApprovedByVerseIds(List.of(1L)))
                .thenReturn(List.of(new ApprovedVerseExplanationResponse(
                        1L,
                        null,
                        "explanation",
                        "source",
                        100L
                )));
        when(glossaryTermRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKeyOrderByBibleVerseIdAscIdAsc(
                List.of(1L),
                GlossaryTermStatus.APPROVED,
                GlossaryTerm.ACTIVE_UNIQUE_KEY
        )).thenReturn(List.of());

        QtStudyContentResponse response = service.getStudyContent(10L);

        assertThat(response.summary()).isNull();
        assertThat(response.glossaryTerms()).isEmpty();
    }

    @Test
    @DisplayName("해설이 없는 절이 있어도 승인 해설만 반환하고 조회 계약을 유지한다")
    void getStudyContent_whenSomeVerseExplanationsAreMissing_returnsOnlyApprovedExplanations() {
        when(getQtPassageContentContextUseCase.getContentContext(10L))
                .thenReturn(context(List.of(1L, 2L, 3L)));
        when(verseExplanationService.listApprovedByVerseIds(List.of(1L, 2L, 3L)))
                .thenReturn(List.of(new ApprovedVerseExplanationResponse(
                        2L,
                        "summary-2",
                        "explanation-2",
                        "source-2",
                        102L
                )));
        when(glossaryTermRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKeyOrderByBibleVerseIdAscIdAsc(
                List.of(1L, 2L, 3L),
                GlossaryTermStatus.APPROVED,
                GlossaryTerm.ACTIVE_UNIQUE_KEY
        )).thenReturn(List.of());

        QtStudyContentResponse response = service.getStudyContent(10L);

        assertThat(response.summary()).isEqualTo("summary-2");
        assertThat(response.explanations())
                .extracting(QtStudyContentResponse.ExplanationItem::verseId)
                .containsExactly(2L);
        assertThat(response.glossaryTerms()).isEmpty();
        verify(verseExplanationService).listApprovedByVerseIds(List.of(1L, 2L, 3L));
    }

    @Test
    @DisplayName("qtPassageId가 1보다 작으면 INVALID_INPUT")
    void getStudyContent_whenInvalidId_throwsInvalidInput() {
        assertThatThrownBy(() -> service.getStudyContent(0L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
    }

    @Test
    @DisplayName("노출 불가 QT 본문이면 QT_PASSAGE_NOT_FOUND")
    void getStudyContent_whenUnpublished_throwsQtPassageNotFound() {
        when(getQtPassageContentContextUseCase.getContentContext(10L))
                .thenReturn(new QtPassageContentContext(
                        10L,
                        LocalDate.of(2026, 5, 28),
                        "test",
                        List.of(1L),
                        false
                ));

        assertThatThrownBy(() -> service.getStudyContent(10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND));
    }

    private static QtPassageContentContext context(List<Long> verseIds) {
        return new QtPassageContentContext(
                10L,
                LocalDate.of(2026, 5, 28),
                "test",
                verseIds,
                true
        );
    }
}
