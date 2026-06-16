package com.qtai.domain.sharing.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** '#닉네임' 멘션 파서 단위 테스트. */
class MentionTextParserTest {

    @Test
    void 한글_영문_여러_멘션을_중복없이_추출한다() {
        var result = MentionTextParser.extractNicknames("안녕 #지혜 그리고 #John #지혜 반가워");
        assertThat(result).containsExactly("지혜", "John");
    }

    @Test
    void 멘션이_없으면_빈집합() {
        assertThat(MentionTextParser.extractNicknames("그냥 본문입니다")).isEmpty();
        assertThat(MentionTextParser.extractNicknames("")).isEmpty();
        assertThat(MentionTextParser.extractNicknames(null)).isEmpty();
    }

    @Test
    void 단독_샵이나_공백뒤_샵은_닉네임이_아니다() {
        // '#' 뒤에 닉네임 문자가 없으면 멘션이 아니다.
        assertThat(MentionTextParser.extractNicknames("# 빈 샵 #")).isEmpty();
    }

    @Test
    void 닉네임은_최대_20자까지만_본다() {
        String long21 = "a".repeat(21);
        var result = MentionTextParser.extractNicknames("#" + long21);
        // 앞 20자만 닉네임으로 인식.
        assertThat(result).containsExactly("a".repeat(20));
    }
}
