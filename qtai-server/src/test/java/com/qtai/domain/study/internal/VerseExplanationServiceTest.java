package com.qtai.domain.study.internal;

import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.qtai.support.TestEntityFactory.verseExplanation;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VerseExplanationServiceTest {

    private final VerseExplanationRepository verseExplanationRepository = mock(VerseExplanationRepository.class);
    private final VerseExplanationService verseExplanationService =
            new VerseExplanationService(verseExplanationRepository);

    @Test
    @DisplayName("승인된 현재 활성 해설만 공개 DTO로 반환한다")
    void listApprovedByVerseIds_returnsApprovedActiveDtos() {
        when(verseExplanationRepository.findByBibleVerseIdInAndStatusAndActiveUniqueKey(
                List.of(10L),
                VerseExplanationStatus.APPROVED,
                "ACTIVE"
        )).thenReturn(List.of(verseExplanation(
                10L,
                VerseExplanationStatus.APPROVED,
                "ACTIVE",
                "summary"
        )));

        List<ApprovedVerseExplanationResponse> responses =
                verseExplanationService.listApprovedByVerseIds(List.of(10L));

        assertThat(responses).containsExactly(new ApprovedVerseExplanationResponse(
                10L,
                "summary",
                "test explanation",
                "test source",
                100L
        ));
    }
}
