package com.qtai.domain.qt.client.sum;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class SuTodayPassageParser {

    private static final Pattern REFERENCE_PATTERN = Pattern.compile(
            "([가-힣0-9ⅠⅡⅢ]+)\\s*\\(([^)]+)\\)\\s*(\\d{1,3})\\s*:\\s*(\\d{1,3})\\s*-\\s*(?:(\\d{1,3})\\s*:\\s*)?(\\d{1,3})"
    );

    public SuTodayPassage parseToday(String html) {
        Matcher matcher = REFERENCE_PATTERN.matcher(plainText(html));
        if (!matcher.find()) {
            throw invalidInput("성서유니온 오늘 본문 범위를 찾을 수 없습니다.");
        }

        short startChapter = parseShort(matcher.group(3), "시작 장");
        short startVerse = parseShort(matcher.group(4), "시작 절");
        short endChapter = matcher.group(5) == null
                ? startChapter
                : parseShort(matcher.group(5), "종료 장");
        short endVerse = parseShort(matcher.group(6), "종료 절");
        if (startChapter != endChapter) {
            throw invalidInput("QT 본문 범위는 같은 장 안에서만 저장할 수 있습니다.");
        }

        String koreanBookName = matcher.group(1).trim();
        String englishBookName = matcher.group(2).trim();
        String referenceText = koreanBookName + "(" + englishBookName + ") "
                + startChapter + ":" + startVerse + "-" + endVerse;
        return new SuTodayPassage(
                referenceText,
                koreanBookName,
                englishBookName,
                startChapter,
                startVerse,
                endVerse,
                referenceText
        );
    }


    private String plainText(String html) {
        return decodeHtmlEntities(html)
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String decodeHtmlEntities(String value) {
        return value
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }

    private short parseShort(String value, String label) {
        try {
            return Short.parseShort(value);
        } catch (NumberFormatException exception) {
            throw invalidInput("성서유니온 " + label + " 값을 파싱할 수 없습니다.");
        }
    }

    private BusinessException invalidInput(String message) {
        return new BusinessException(ErrorCode.INVALID_INPUT, message);
    }
}
