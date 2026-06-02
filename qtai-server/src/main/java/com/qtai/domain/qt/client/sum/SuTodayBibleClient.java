package com.qtai.domain.qt.client.sum;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class SuTodayBibleClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final URI todayUrl;
    private final SuTodayPassageParser parser;

    @Autowired
    public SuTodayBibleClient(
            @Value("${qt.today-source.sum.url:https://sum.su.or.kr:8888/bible/today}") String todayUrl,
            SuTodayPassageParser parser
    ) {
        this(HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(5))
                        .build(),
                URI.create(todayUrl),
                parser);
    }

    SuTodayBibleClient(HttpClient httpClient, URI todayUrl, SuTodayPassageParser parser) {
        this.httpClient = httpClient;
        this.todayUrl = todayUrl;
        this.parser = parser;
    }

    public SuTodayPassage fetchToday() {
        HttpRequest request = HttpRequest.newBuilder(todayUrl)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "QT-AI/1.0")
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("성서유니온 오늘 본문 조회 실패. status=" + response.statusCode());
            }
            return parser.parseToday(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("성서유니온 오늘 본문 조회 중 네트워크 오류가 발생했습니다.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("성서유니온 오늘 본문 조회가 중단되었습니다.", exception);
        }
    }
}
