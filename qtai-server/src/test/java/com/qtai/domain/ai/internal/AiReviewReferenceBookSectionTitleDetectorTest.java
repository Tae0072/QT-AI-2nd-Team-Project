package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiReviewReferenceBookSectionTitleDetectorTest {

    private final AiReviewReferenceBookSectionTitleDetector detector =
            new AiReviewReferenceBookSectionTitleDetector();

    @Test
    void detectsExactBookTitleLine() {
        AiReviewReferenceBookSectionTitleDetector.DetectedTitle title =
                detector.detect("   마태복음   ").orElseThrow();

        assertThat(title.book().code()).isEqualTo("MAT");
        assertThat(title.book().koreanName()).isEqualTo("마태복음");
        assertThat(title.confidence()).isEqualTo(AiReviewReferenceBookSectionMapCandidateWriter.Confidence.HIGH);
        assertThat(title.reasons()).containsExactly("EXACT_BOOK_TITLE");
    }

    @Test
    void doesNotDetectBookNameInsideBodyTextAsTitle() {
        assertThat(detector.detect("마태복음은 예수님의 족보로 시작한다는 본문 설명입니다.")).isEmpty();
    }

    @Test
    void marksWeakTitleLineAsLowConfidence() {
        AiReviewReferenceBookSectionTitleDetector.DetectedTitle title =
                detector.detect("마태복음 개론").orElseThrow();

        assertThat(title.book().code()).isEqualTo("MAT");
        assertThat(title.confidence()).isEqualTo(AiReviewReferenceBookSectionMapCandidateWriter.Confidence.LOW);
        assertThat(title.reasons()).containsExactly("WEAK_BOOK_TITLE_LINE");
    }
}
