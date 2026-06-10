package com.qtai.domain.note.client.qt;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link NoteQtRestClientAdapter} 단위 테스트 — MockRestServiceServer로 service-bible(qt) 호출을 흉내낸다.
 *
 * <p>검증: 읽기 가능(2xx)이면 통과, 404→{@link ErrorCode#QT_PASSAGE_NOT_FOUND},
 * 403→{@link ErrorCode#FORBIDDEN}, 5xx→{@link ErrorCode#EXTERNAL_API_FAILURE},
 * 입력 가드(qtPassageId null/<1)는 HTTP 호출 없이 QT_PASSAGE_NOT_FOUND,
 * 요청의 Authorization 헤더를 qt 호출에 그대로 전달.
 */
class NoteQtRestClientAdapterTest {

    private static final String BIBLE_BASE_URL = "http://bible.test";

    private MockRestServiceServer server;
    private NoteQtRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ServiceEndpointsProperties endpoints = new ServiceEndpointsProperties();
        endpoints.setBibleBaseUrl(BIBLE_BASE_URL);
        adapter = new NoteQtRestClientAdapter(builder, endpoints);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("본문이 읽기 가능(2xx)하면 예외 없이 통과한다")
    void 읽기가능_통과() {
        server.expect(requestTo(BIBLE_BASE_URL + "/api/v1/qt/passages/5"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess());

        assertThatCode(() -> adapter.validateReadable(123L, 5L)).doesNotThrowAnyException();
        server.verify();
    }

    @Test
    @DisplayName("404는 QT_PASSAGE_NOT_FOUND로 변환한다")
    void 응답_404() {
        server.expect(requestTo(BIBLE_BASE_URL + "/api/v1/qt/passages/9"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.validateReadable(123L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND);
    }

    @Test
    @DisplayName("403은 FORBIDDEN으로 변환한다")
    void 응답_403() {
        server.expect(requestTo(BIBLE_BASE_URL + "/api/v1/qt/passages/9"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN));

        assertThatThrownBy(() -> adapter.validateReadable(123L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.FORBIDDEN);
    }

    @Test
    @DisplayName("5xx는 EXTERNAL_API_FAILURE로 변환한다")
    void 응답_5xx() {
        server.expect(requestTo(BIBLE_BASE_URL + "/api/v1/qt/passages/9"))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.validateReadable(123L, 9L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("잘못된 입력은 HTTP 호출 없이 QT_PASSAGE_NOT_FOUND")
    void 입력_가드() {
        assertThatThrownBy(() -> adapter.validateReadable(123L, 0L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.QT_PASSAGE_NOT_FOUND);
        assertThatThrownBy(() -> adapter.validateReadable(null, 5L))
                .isInstanceOf(BusinessException.class);
        server.verify(); // 기대 요청 없음 = 호출 없었음
    }

    @Test
    @DisplayName("요청의 Authorization 헤더를 qt 호출에 그대로 전달한다")
    void 인증헤더_전달() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        server.expect(requestTo(BIBLE_BASE_URL + "/api/v1/qt/passages/5"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess());

        adapter.validateReadable(123L, 5L);
        server.verify();
    }
}
