package com.qtai.domain.study.internal;

import com.qtai.domain.study.api.dto.ApprovedVerseExplanationResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

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
        )).thenReturn(List.of(explanation()));

        List<ApprovedVerseExplanationResponse> responses =
                verseExplanationService.listApprovedByVerseIds(List.of(10L));

        assertThat(responses).containsExactly(new ApprovedVerseExplanationResponse(
                10L,
                "요약",
                "검증용 해설",
                "테스트 출처",
                100L
        ));
    }

    private static VerseExplanation explanation() {
        VerseExplanation explanation = newInstance(VerseExplanation.class);
        set(explanation, "bibleVerseId", 10L);
        set(explanation, "summary", "요약");
        set(explanation, "explanation", "검증용 해설");
        set(explanation, "sourceLabel", "테스트 출처");
        set(explanation, "status", VerseExplanationStatus.APPROVED);
        set(explanation, "activeUniqueKey", "ACTIVE");
        set(explanation, "aiAssetId", 100L);
        return explanation;
    }

    private static <T> T newInstance(Class<T> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void set(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
