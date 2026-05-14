package com.qtai.bff.usecase;

import com.qtai.bff.client.BibleClient;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 오늘의 QT 미리보기 UseCase.
 *
 * <p>비로그인 OK. 본문(한국어/영어) + 본문 설명 요약을 병렬로 조립.
 *
 * <p>TODO(강태오): 오늘 QT 본문을 어디서 받아오는지 결정 — 성서 유니온 스크래핑 (19:00)
 *               결과를 Bible Service나 별도 스케줄러가 적재. 현재는 GEN 1:1 고정 더미.
 */
@Service
public class TodayQtUseCase {

    private final BibleClient bibleClient;

    public TodayQtUseCase(BibleClient bibleClient) {
        this.bibleClient = bibleClient;
    }

    public Map<String, Object> execute() {
        // TODO: 오늘 QT 본문 ref를 환경/DB에서 받아오기 (현재 GEN 1:1 고정 더미)
        String bookCode = "GEN";
        int chapter = 1, verse = 1;

        CompletableFuture<Map<?, ?>> kr = bibleClient.getKr(bookCode, chapter, verse);
        CompletableFuture<Map<?, ?>> en = bibleClient.getEn(bookCode, chapter, verse);
        CompletableFuture<Map<?, ?>> exp = bibleClient.getExplanation(bookCode, chapter, verse);

        CompletableFuture.allOf(kr, en, exp).join();

        return Map.of(
                "qtDate", LocalDate.now().toString(),
                "passage", Map.of("bookCode", bookCode, "chapter", chapter, "verse", verse),
                "kr", kr.join(),
                "en", en.join(),
                "explanation", exp.join()
        );
    }
}
