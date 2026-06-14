package com.qtai.domain.sharing.internal;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 본문에서 '#닉네임' 멘션 토큰을 뽑아내는 파서.
 *
 * <p>닉네임은 한글·영문·숫자·밑줄 1~20자(회원 닉네임 제약과 동일). '#' 바로 뒤부터 매칭한다.
 * 같은 닉네임이 여러 번 나와도 한 번만 반환한다(입력 순서 유지). 실제로 존재하는 회원인지는
 * 호출부에서 닉네임→회원 해석으로 확인한다(여기선 형태만 추출).
 */
final class MentionTextParser {

    // '#' + (한글/영문/숫자/_) 1~20자. 닉네임 최대 길이 20과 일치.
    private static final Pattern MENTION = Pattern.compile("#([0-9A-Za-z가-힣_]{1,20})");

    private MentionTextParser() {
    }

    /** 본문에서 멘션 닉네임 후보를 중복 없이 추출한다. 빈/널 입력이면 빈 집합. */
    static Set<String> extractNicknames(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        Set<String> result = new LinkedHashSet<>();
        Matcher matcher = MENTION.matcher(text);
        while (matcher.find()) {
            result.add(matcher.group(1));
        }
        return result;
    }
}
