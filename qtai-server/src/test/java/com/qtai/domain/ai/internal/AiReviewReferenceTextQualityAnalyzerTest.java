package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AiReviewReferenceTextQualityAnalyzerTest {

    private final AiReviewReferenceTextQualityAnalyzer analyzer = new AiReviewReferenceTextQualityAnalyzer();

    @Test
    void normalKoreanTextIsUsableWithLowErrorRates() {
        String text = "이 문단은 성경 배경 설명을 위한 정상적인 한글 문장입니다. "
                + "문맥과 역사적 배경을 설명하며 깨진 문자가 거의 없습니다.";

        AiReviewReferenceTextQualityAnalyzer.QualityResult result = analyzer.analyze(text);

        assertThat(result.status()).isEqualTo(AiReviewReferenceTextQualityAnalyzer.QualityStatus.USABLE);
        assertThat(result.replacementCharCount()).isZero();
        assertThat(result.hangulRatio()).isGreaterThan(0.3);
    }

    @Test
    void normalEnglishTextDoesNotBecomeUnusableByItself() {
        String text = "This paragraph contains readable background commentary text without broken glyph markers.";

        AiReviewReferenceTextQualityAnalyzer.QualityResult result = analyzer.analyze(text);

        assertThat(result.status()).isIn(
                AiReviewReferenceTextQualityAnalyzer.QualityStatus.USABLE,
                AiReviewReferenceTextQualityAnalyzer.QualityStatus.NEEDS_REVIEW
        );
        assertThat(result.reasons()).doesNotContain("MOJIBAKE_RATIO_HIGH");
    }

    @Test
    void manyMojibakeMarkersNeedReviewOrBecomeUnusable() {
        String text = ("占竊���???? 깨진 문자 ").repeat(12);

        AiReviewReferenceTextQualityAnalyzer.QualityResult result = analyzer.analyze(text);

        assertThat(result.status()).isIn(
                AiReviewReferenceTextQualityAnalyzer.QualityStatus.NEEDS_REVIEW,
                AiReviewReferenceTextQualityAnalyzer.QualityStatus.UNUSABLE
        );
        assertThat(result.replacementCharCount()).isGreaterThan(0);
        assertThat(result.suspiciousMojibakeRatio()).isGreaterThan(0);
        assertThat(result.reasons()).isNotEmpty();
    }

    @Test
    void tooShortAndTooLongTextRecordReasons() {
        AiReviewReferenceTextQualityAnalyzer.QualityResult shortResult = analyzer.analyze("짧음");
        AiReviewReferenceTextQualityAnalyzer.QualityResult longResult = analyzer.analyze("가".repeat(12_001));

        assertThat(shortResult.status()).isEqualTo(AiReviewReferenceTextQualityAnalyzer.QualityStatus.UNUSABLE);
        assertThat(shortResult.reasons()).contains("TEXT_TOO_SHORT");
        assertThat(longResult.status()).isEqualTo(AiReviewReferenceTextQualityAnalyzer.QualityStatus.NEEDS_REVIEW);
        assertThat(longResult.reasons()).contains("TEXT_TOO_LONG");
    }
}
