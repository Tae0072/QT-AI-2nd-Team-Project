package com.qtai.domain.sharing.client.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.qtai.common.config.ServiceEndpointsProperties;
import com.qtai.common.exception.BusinessException;
import com.qtai.common.exception.ErrorCode;
import com.qtai.domain.member.api.dto.MemberPublicResponse;
import com.qtai.domain.member.api.dto.MemberResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * {@link MemberRestClientAdapter} 단위 테스트 — MockRestServiceServer로 service-user 호출을 흉내낸다.
 *
 * <p>검증: 공개 프로필 단건/벌크 매핑, getMember는 공개 뷰만(비공개 필드 null),
 * 빈 목록은 HTTP 호출 없이 빈 결과, 404→{@link ErrorCode#MEMBER_NOT_FOUND},
 * 5xx→{@link ErrorCode#EXTERNAL_API_FAILURE}, Authorization 헤더 전달.
 */
class MemberRestClientAdapterTest {

    private static final String USER_BASE_URL = "http://user.test";

    private MockRestServiceServer server;
    private MemberRestClientAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        ServiceEndpointsProperties endpoints = new ServiceEndpointsProperties();
        endpoints.setUserBaseUrl(USER_BASE_URL);
        adapter = new MemberRestClientAdapter(builder, endpoints);
    }

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    @DisplayName("공개 프로필 단건 조회를 매핑한다")
    void 단건_정상() {
        server.expect(requestTo(USER_BASE_URL + "/api/v1/members/7"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"id\":7,\"nickname\":\"닉7\",\"profileImageUrl\":null}}",
                        MediaType.APPLICATION_JSON));

        MemberPublicResponse member = adapter.getMemberPublic(7L);

        assertThat(member.id()).isEqualTo(7L);
        assertThat(member.nickname()).isEqualTo("닉7");
        server.verify();
    }

    @Test
    @DisplayName("벌크 공개 프로필 조회 — members?ids= 엔드포인트를 호출한다")
    void 벌크_정상() {
        server.expect(requestTo(startsWith(USER_BASE_URL + "/api/v1/members?ids=")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":[{\"id\":1,\"nickname\":\"닉1\",\"profileImageUrl\":null},"
                                + "{\"id\":2,\"nickname\":\"닉2\",\"profileImageUrl\":null}]}",
                        MediaType.APPLICATION_JSON));

        List<MemberPublicResponse> members = adapter.getActivePublicProfiles(List.of(1L, 2L));

        assertThat(members).hasSize(2);
        assertThat(members.get(0).nickname()).isEqualTo("닉1");
        server.verify();
    }

    @Test
    @DisplayName("getMember(전체 계약)는 공개 뷰만 채우고 비공개 필드는 null")
    void 전체_계약은_공개뷰() {
        server.expect(requestTo(USER_BASE_URL + "/api/v1/members/7"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"id\":7,\"nickname\":\"닉7\",\"profileImageUrl\":\"img\"}}",
                        MediaType.APPLICATION_JSON));

        MemberResponse member = adapter.getMember(7L);

        assertThat(member.id()).isEqualTo(7L);
        assertThat(member.nickname()).isEqualTo("닉7");
        assertThat(member.profileImageUrl()).isEqualTo("img");
        assertThat(member.email()).isNull();
        assertThat(member.status()).isNull();
        assertThat(member.role()).isNull();
    }

    @Test
    @DisplayName("빈 목록은 HTTP 호출 없이 빈 결과")
    void 빈_목록_단락() {
        assertThat(adapter.getActivePublicProfiles(List.of())).isEmpty();
        server.verify();
    }

    @Test
    @DisplayName("404는 MEMBER_NOT_FOUND로 변환한다")
    void 응답_404() {
        server.expect(requestTo(USER_BASE_URL + "/api/v1/members/99"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.getMemberPublic(99L))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("5xx는 EXTERNAL_API_FAILURE로 변환한다")
    void 응답_5xx() {
        server.expect(requestTo(startsWith(USER_BASE_URL + "/api/v1/members")))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> adapter.getActivePublicProfiles(List.of(1L)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.EXTERNAL_API_FAILURE);
    }

    @Test
    @DisplayName("요청의 Authorization 헤더를 user 호출에 그대로 전달한다")
    void 인증헤더_전달() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer test-token");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        server.expect(requestTo(USER_BASE_URL + "/api/v1/members/7"))
                .andExpect(header("Authorization", "Bearer test-token"))
                .andRespond(withSuccess(
                        "{\"success\":true,\"data\":{\"id\":7,\"nickname\":\"닉7\",\"profileImageUrl\":null}}",
                        MediaType.APPLICATION_JSON));

        adapter.getMemberPublic(7L);
        server.verify();
    }
}
