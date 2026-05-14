package com.qtai.bff.usecase;

import com.qtai.bff.client.BibleClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 입체 묵상 화면 UseCase (소프트 로그인).
 *
 * <p>비로그인: KR + EN + 본문 설명만.
 * <p>로그인:   + 해설(commentary) + 사용자 묵상/AI 세션 상태 부착.
 *
 * <p>참조: apis/bible/openapi.yaml — Bible Facade 병렬 호출 패턴.
 */
@Service
public class PassageUseCase {

    private final BibleClient bibleClient;

    public PassageUseCase(BibleClient bibleClient) {
        this.bibleClient = bibleClient;
    }

    public Map<String, Object> execute(String bookCode, int chapter, int verse, String bearer) {
        CompletableFuture<Map<?, ?>> kr = bibleClient.getKr(bookCode, chapter, verse);
        CompletableFuture<Map<?, ?>> en = bibleClient.getEn(bookCode, chapter, verse);
        CompletableFuture<Map<?, ?>> exp = bibleClient.getExplanation(bookCode, chapter, verse);

        CompletableFuture<Map<?, ?>> commentary = null;
        if (bearer != null && bearer.startsWith("Bearer ")) {
            commentary = bibleClient.getCommentary(bookCode, chapter, verse, bearer);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("passage", Map.of("bookCode", bookCode, "chapter", chapter, "verse", verse));
        result.put("kr", kr.join());
        result.put("en", en.join());
        result.put("explanation", exp.join());
        if (commentary != null) {
            result.put("commentary", commentary.join());
        }
        return result;
    }
}
