package com.qtai.domain.qt.client.sum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuTodayBibleClientTest {

    private final SuTodayPassageParser parser = new SuTodayPassageParser();

    @Test
    @DisplayName("성서유니온 HTML 응답에서 오늘 QT 제목과 범위를 반환한다")
    void fetchToday_parsesSuccessfulResponse() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(200, """
                <div class="bible_text" id="bible_text">같은 말, 같은 마음, 같은 뜻</div>
                <div class="bibleinfo_box" id="bibleinfo_box">
                    본문 : 고린도전서(1 Corinthians) 1:10 - 1:17 찬송가 455장
                </div>
                """);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        SuTodayBibleClient client = new SuTodayBibleClient(
                httpClient,
                URI.create("https://example.test/bible/today"),
                parser
        );

        SuTodayPassage result = client.fetchToday();

        assertThat(result.title()).isEqualTo("같은 말, 같은 마음, 같은 뜻");
        assertThat(result.englishBookName()).isEqualTo("1 Corinthians");
        assertThat(result.referenceText()).isEqualTo("고린도전서(1 Corinthians) 1:10-17");
    }

    @Test
    @DisplayName("성서유니온 응답이 2xx가 아니면 실패한다")
    void fetchToday_rejectsNonSuccessStatus() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> response = mockResponse(500, "server error");
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenReturn(response);
        SuTodayBibleClient client = new SuTodayBibleClient(
                httpClient,
                URI.create("https://example.test/bible/today"),
                parser
        );

        assertThatThrownBy(client::fetchToday)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("status=500");
    }

    @Test
    @DisplayName("성서유니온 네트워크 오류는 공통 실패 예외로 감싼다")
    void fetchToday_wrapsNetworkFailure() throws Exception {
        HttpClient httpClient = mock(HttpClient.class);
        when(httpClient.send(any(), any(HttpResponse.BodyHandler.class)))
                .thenThrow(new IOException("network down"));
        SuTodayBibleClient client = new SuTodayBibleClient(
                httpClient,
                URI.create("https://example.test/bible/today"),
                parser
        );

        assertThatThrownBy(client::fetchToday)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("네트워크 오류")
                .hasCauseInstanceOf(IOException.class);
    }

    private HttpResponse<String> mockResponse(int statusCode, String body) {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        return response;
    }
}
