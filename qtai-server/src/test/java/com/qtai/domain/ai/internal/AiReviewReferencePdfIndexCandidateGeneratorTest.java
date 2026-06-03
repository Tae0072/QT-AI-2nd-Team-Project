package com.qtai.domain.ai.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class AiReviewReferencePdfIndexCandidateGeneratorTest {

    private final AiReviewReferencePdfIndexCandidateGenerator generator =
            new AiReviewReferencePdfIndexCandidateGenerator(
                    new AiReviewReferencePdfHeadingParser(),
                    new AiReviewReferenceTextQualityAnalyzer()
            );

    @Test
    void splitsEntriesByPureHeadingLines() {
        List<AiReviewReferencePdfIndexCandidateWriter.CandidateEntry> entries = generator.entriesFromPages(List.of(
                new AiReviewReferencePdfIndexCandidateGenerator.PageText(3, """
                        19:8
                        첫 번째 후보 본문입니다. 충분한 길이를 위해 문장을 더 붙입니다.
                        19:11-15
                        두 번째 후보 본문입니다. 충분한 길이를 위해 문장을 더 붙입니다.
                        """)
        ));

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).detectedHeading()).isEqualTo("19:8");
        assertThat(entries.get(0).pageStart()).isEqualTo(3);
        assertThat(entries.get(0).referenceText()).contains("첫 번째 후보 본문");
        assertThat(entries.get(1).detectedHeading()).isEqualTo("19:11-15");
        assertThat(entries.get(1).referenceText()).contains("두 번째 후보 본문");
    }

    @Test
    void keepsBodyTextAcrossPageBoundaryUntilNextHeading() {
        List<AiReviewReferencePdfIndexCandidateWriter.CandidateEntry> entries = generator.entriesFromPages(List.of(
                new AiReviewReferencePdfIndexCandidateGenerator.PageText(10, """
                        19:42-20:6
                        페이지 끝에 시작된 후보 본문입니다.
                        """),
                new AiReviewReferencePdfIndexCandidateGenerator.PageText(11, """
                        다음 페이지로 이어지는 후보 본문입니다.
                        20:7
                        다음 heading의 후보 본문입니다.
                        """)
        ));

        assertThat(entries).hasSize(2);
        assertThat(entries.get(0).pageStart()).isEqualTo(10);
        assertThat(entries.get(0).detectedHeading()).isEqualTo("19:42-20:6");
        assertThat(entries.get(0).referenceText())
                .contains("페이지 끝에 시작된 후보 본문입니다.")
                .contains("다음 페이지로 이어지는 후보 본문입니다.");
        assertThat(entries.get(1).pageStart()).isEqualTo(11);
        assertThat(entries.get(1).detectedHeading()).isEqualTo("20:7");
    }

    @Test
    void doesNotSplitBodyLineThatStartsWithVerseReferenceAndText() {
        List<AiReviewReferencePdfIndexCandidateWriter.CandidateEntry> entries = generator.entriesFromPages(List.of(
                new AiReviewReferencePdfIndexCandidateGenerator.PageText(5, """
                        19:8
                        19:8 this body line mentions a verse reference but is not a heading.
                        이어지는 후보 본문입니다.
                        """)
        ));

        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).detectedHeading()).isEqualTo("19:8");
        assertThat(entries.get(0).referenceText())
                .contains("19:8 this body line mentions a verse reference")
                .contains("이어지는 후보 본문입니다.");
    }
}
