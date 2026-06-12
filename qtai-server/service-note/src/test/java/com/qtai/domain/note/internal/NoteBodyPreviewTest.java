package com.qtai.domain.note.internal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.qtai.domain.note.internal.NoteService.BODY_PREVIEW_MAX;
import static com.qtai.domain.note.internal.NoteService.previewOf;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * 노트 목록 카드 미리보기 생성({@link NoteService#previewOf}) 단위 테스트.
 * 자유노트 body 우선 / 섹션 폴백 / 공백 정리 / 80자 truncate / 서로게이트 페어 보호.
 */
class NoteBodyPreviewTest {

    @Test
    @DisplayName("모든 입력이 비어 있으면 null")
    void allBlankReturnsNull() {
        assertThat(previewOf(null, null, null, null, null)).isNull();
        assertThat(previewOf("", "  ", "\n", null, "\t")).isNull();
    }

    @Test
    @DisplayName("body가 있으면 body를 우선 사용한다")
    void usesBodyFirst() {
        assertThat(previewOf("자유노트 본문", "기억", null, null, null))
                .isEqualTo("자유노트 본문");
    }

    @Test
    @DisplayName("body가 비면 섹션을 순서대로 폴백한다")
    void fallsBackToSectionsInOrder() {
        assertThat(previewOf(null, "기억할 구절", "해석", null, null))
                .isEqualTo("기억할 구절");
        assertThat(previewOf(null, null, null, null, "기도 내용"))
                .isEqualTo("기도 내용");
    }

    @Test
    @DisplayName("줄바꿈·연속 공백은 한 칸으로 합친다")
    void collapsesWhitespace() {
        assertThat(previewOf("첫 줄\n\n둘째   줄\t끝", null, null, null, null))
                .isEqualTo("첫 줄 둘째 줄 끝");
    }

    @Test
    @DisplayName("길이가 한도 이하면 그대로 반환한다")
    void shortReturnedAsIs() {
        String s = "가".repeat(BODY_PREVIEW_MAX);
        assertThat(previewOf(s, null, null, null, null)).isEqualTo(s);
    }

    @Test
    @DisplayName("한도를 넘으면 잘라내고 말줄임표를 붙인다")
    void truncatesWithEllipsis() {
        String s = "가".repeat(BODY_PREVIEW_MAX + 10);
        assertThat(previewOf(s, null, null, null, null))
                .isEqualTo("가".repeat(BODY_PREVIEW_MAX) + "…");
    }

    @Test
    @DisplayName("서로게이트 페어(이모지)를 경계에서 쪼개지 않는다")
    void doesNotSplitSurrogatePair() {
        // 80번째 글자(index 79)에 이모지의 상위 서로게이트가 오도록 구성한다.
        String s = "a".repeat(BODY_PREVIEW_MAX - 1) + "😀" + "b";
        // 이모지를 가운데서 자르면 외톨이 서로게이트가 남는다 → 한 글자 앞에서 잘라야 한다.
        assertThat(previewOf(s, null, null, null, null))
                .isEqualTo("a".repeat(BODY_PREVIEW_MAX - 1) + "…");
    }
}
