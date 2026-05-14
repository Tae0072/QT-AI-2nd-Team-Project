package com.qtai.bff.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Bible Service RestClient.
 *
 * <p>BFF는 입체 묵상 화면을 위해 KR / EN / Commentary 3개를 CompletableFuture 병렬 호출한다.
 *
 * <p>TODO(강태오): timeout 2s, retry 1회, ProblemDetail 매핑.
 */
@Component
public class BibleClient {

    private final RestClient client;

    public BibleClient(@Value("${bff.bible-service.url:http://bible-service:8082}") String baseUrl) {
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Async("bibleExecutor")
    public CompletableFuture<Map<?, ?>> getKr(String bookCode, int chapter, int verse) {
        Map<?, ?> body = client.get().uri("/bible/kr/{b}/{c}/{v}", bookCode, chapter, verse)
                .retrieve().body(Map.class);
        return CompletableFuture.completedFuture(body);
    }

    @Async("bibleExecutor")
    public CompletableFuture<Map<?, ?>> getEn(String bookCode, int chapter, int verse) {
        Map<?, ?> body = client.get().uri("/bible/en/{b}/{c}/{v}", bookCode, chapter, verse)
                .retrieve().body(Map.class);
        return CompletableFuture.completedFuture(body);
    }

    @Async("bibleExecutor")
    public CompletableFuture<Map<?, ?>> getCommentary(String bookCode, int chapter, int verse, String bearer) {
        Map<?, ?> body = client.get().uri("/api/v1/commentary/{b}/{c}/{v}", bookCode, chapter, verse)
                .header("Authorization", bearer)
                .retrieve().body(Map.class);
        return CompletableFuture.completedFuture(body);
    }

    @Async("bibleExecutor")
    public CompletableFuture<Map<?, ?>> getExplanation(String bookCode, int chapter, int verse) {
        Map<?, ?> body = client.get().uri("/api/v1/explanations/{b}/{c}/{v}", bookCode, chapter, verse)
                .retrieve().body(Map.class);
        return CompletableFuture.completedFuture(body);
    }
}
