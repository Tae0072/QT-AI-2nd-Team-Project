package com.qtai.domain.study.internal;

import com.qtai.domain.study.api.ListApprovedVerseExplanationUseCase;
import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VerseExplanationService implements ListApprovedVerseExplanationUseCase {

    private static final String ACTIVE_UNIQUE_KEY = "ACTIVE";

    private final VerseExplanationRepository verseExplanationRepository;

    @Override
    public List<ApprovedVerseExplanationResponse> listApprovedByVerseIds(List<Long> verseIds) {
        if (verseIds == null || verseIds.isEmpty()) {
            return List.of();
        }

        return verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                        verseIds,
                        VerseExplanationStatus.APPROVED,
                        ACTIVE_UNIQUE_KEY
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private ApprovedVerseExplanationResponse toResponse(VerseExplanation explanation) {
        return new ApprovedVerseExplanationResponse(
                explanation.getBibleVerseId(),
                explanation.getSummary(),
                explanation.getExplanation(),
                explanation.getSourceLabel(),
                explanation.getAiAssetId()
        );
    }
}
