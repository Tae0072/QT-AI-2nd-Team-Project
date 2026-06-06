package com.qtai.domain.study.internal;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.qt.api.GetQtPassageContentContextUseCase;
import com.qtai.domain.qt.api.dto.QtPassageContentContext;
import com.qtai.domain.study.api.GetQtStudyContentUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import com.qtai.domain.study.api.dto.QtStudyContentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QtStudyContentService implements GetQtStudyContentUseCase {

    private final GetQtPassageContentContextUseCase getQtPassageContentContextUseCase;
    private final VerseExplanationService verseExplanationService;
    private final GlossaryTermRepository glossaryTermRepository;

    @Override
    public QtStudyContentResponse getStudyContent(Long qtPassageId) {
        validateQtPassageId(qtPassageId);
        QtPassageContentContext context = getQtPassageContentContextUseCase.getContentContext(qtPassageId);
        if (!context.published()) {
            throw new BusinessException(ErrorCode.QT_PASSAGE_NOT_FOUND);
        }

        List<Long> verseIds = context.verseIds() == null ? List.of() : context.verseIds();
        Map<Long, Integer> verseOrder = verseOrder(verseIds);
        List<QtStudyContentResponse.ExplanationItem> explanations = verseExplanationService
                .listApprovedByVerseIds(verseIds)
                .stream()
                .sorted(Comparator.comparingInt(response -> verseOrder.getOrDefault(response.verseId(), Integer.MAX_VALUE)))
                .map(this::toExplanationItem)
                .toList();
        List<QtStudyContentResponse.GlossaryTermItem> glossaryTerms = glossaryTermRepository
                .findByBibleVerseIdInAndStatusOrderByBibleVerseIdAscIdAsc(verseIds, GlossaryTermStatus.APPROVED)
                .stream()
                .sorted(Comparator.comparingInt(term -> verseOrder.getOrDefault(term.getBibleVerseId(), Integer.MAX_VALUE)))
                .map(this::toGlossaryTermItem)
                .toList();

        return new QtStudyContentResponse(summaryFrom(explanations), explanations, glossaryTerms);
    }

    private static void validateQtPassageId(Long qtPassageId) {
        if (qtPassageId == null || qtPassageId < 1) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private static Map<Long, Integer> verseOrder(List<Long> verseIds) {
        return java.util.stream.IntStream.range(0, verseIds.size())
                .boxed()
                .collect(Collectors.toMap(verseIds::get, Function.identity(), (left, right) -> left));
    }

    private QtStudyContentResponse.ExplanationItem toExplanationItem(ApprovedVerseExplanationResponse response) {
        // response.aiAssetId()는 내부 추적용 — 사용자 응답 DTO로 전달하지 않는다(P2).
        return new QtStudyContentResponse.ExplanationItem(
                response.verseId(),
                response.summary(),
                response.explanation(),
                response.sourceLabel()
        );
    }

    private QtStudyContentResponse.GlossaryTermItem toGlossaryTermItem(GlossaryTerm term) {
        return new QtStudyContentResponse.GlossaryTermItem(
                term.getId(),
                term.getBibleVerseId(),
                term.getTerm(),
                term.getMeaning(),
                term.getSourceLabel()
        );
    }

    private String summaryFrom(List<QtStudyContentResponse.ExplanationItem> explanations) {
        String summary = explanations.stream()
                .map(QtStudyContentResponse.ExplanationItem::summary)
                .filter(value -> value != null && !value.isBlank())
                .limit(3)
                .collect(Collectors.joining(" "));
        return summary.isBlank() ? null : summary;
    }
}
