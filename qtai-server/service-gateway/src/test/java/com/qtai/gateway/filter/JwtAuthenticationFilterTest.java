package com.qtai.gateway.filter;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.security.JwtTokenVerifier;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 게이트웨이 JWT 인증 필터 단위 테스트.
 *
 * <p>스프링 컨텍스트를 띄우지 않고 런타임 생성 RSA 키쌍으로 토큰을 서명·검증한다(빠르고 결정적).
 * 컨텍스트 로드용 정적 공개키(test application.properties)와는 별개다.
 */
class JwtAuthenticationFilterTest {

    private static KeyPair signingPair;   // verifier 공개키와 짝이 맞는 정상 서명 키
    private static KeyPair attackerPair;  // 서명 불일치(변조) 재현용

    private JwtAuthenticationFilter filter;

    @BeforeAll
    static void generateKeys() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        signingPair = gen.generateKeyPair();
        attackerPair = gen.generateKeyPair();
    }

    @BeforeEach
    void setUp() {
        JwtTokenVerifier verifier = new JwtTokenVerifier(signingPair.getPublic());
        ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules(); // OffsetDateTime(jsr310)
        filter = new JwtAuthenticationFilter(verifier, objectMapper);
    }

    // ── 토큰 빌더 헬퍼 ──

    private String token(String sub, String role, String type, long expOffsetMillis, PrivateKey key) {
        return Jwts.builder()
                .subject(sub)
                .claim("role", role)
                .claim("type", type)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expOffsetMillis))
                .signWith(key)
                .compact();
    }

    private String validAccess(String sub, String role) {
        return token(sub, role, "access", 60_000, signingPair.getPrivate());
    }

    /** exchange를 포착하고 더 진행하지 않는 체인(다운스트림 전달 여부·헤더 확인용). */
    private WebFilterChain capturing(AtomicReference<ServerWebExchange> sink) {
        return exchange -> {
            sink.set(exchange);
            return Mono.empty();
        };
    }

    // ── 통과 + 신원 헤더 주입 ──

    @Test
    @DisplayName("유효한 access token → 통과 + X-Member-Id/Role 주입")
    void validToken_passes_and_injects_identity() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAccess("42", "USER")));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        assertThat(captured.get()).isNotNull(); // 다운스트림으로 전달됨
        HttpHeaders forwarded = captured.get().getRequest().getHeaders();
        assertThat(forwarded.getFirst(JwtAuthenticationFilter.HEADER_MEMBER_ID)).isEqualTo("42");
        assertThat(forwarded.getFirst(JwtAuthenticationFilter.HEADER_MEMBER_ROLE)).isEqualTo("USER");
        assertThat(exchange.getResponse().getStatusCode()).isNull(); // 401 미설정
    }

    @Test
    @DisplayName("유효 토큰 + 클라이언트가 위조한 X-Member-Id → 검증값으로 덮어씀")
    void validToken_overrides_spoofed_identity_header() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + validAccess("42", "USER"))
                        .header(JwtAuthenticationFilter.HEADER_MEMBER_ID, "999")
                        .header(JwtAuthenticationFilter.HEADER_MEMBER_ROLE, "ADMIN"));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        HttpHeaders forwarded = captured.get().getRequest().getHeaders();
        assertThat(forwarded.getFirst(JwtAuthenticationFilter.HEADER_MEMBER_ID)).isEqualTo("42");
        assertThat(forwarded.getFirst(JwtAuthenticationFilter.HEADER_MEMBER_ROLE)).isEqualTo("USER");
    }

    // ── 401 차단 ──

    @Test
    @DisplayName("Authorization 헤더 없음 → 401 + M0002, 다운스트림 미전달")
    void missing_authorization_returns_401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notes"));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        assertThat(captured.get()).isNull(); // 전달 안 됨
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        String body = exchange.getResponse().getBodyAsString().block();
        assertThat(body).contains("\"success\":false").contains("M0002");
    }

    @Test
    @DisplayName("Bearer 접두사 없는 Authorization → 401")
    void malformed_authorization_returns_401() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Token abc.def.ghi"));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        assertThat(captured.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("만료된 토큰 → 401")
    void expired_token_returns_401() {
        String expired = token("42", "USER", "access", -1_000, signingPair.getPrivate());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + expired));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        assertThat(captured.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("서명 불일치(변조) 토큰 → 401")
    void tampered_signature_returns_401() {
        String forged = token("42", "USER", "access", 60_000, attackerPair.getPrivate());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + forged));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        assertThat(captured.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("refresh 타입 토큰을 인증에 사용 → 401")
    void refresh_token_returns_401() {
        String refresh = token("42", "USER", "refresh", 60_000, signingPair.getPrivate());
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/notes")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + refresh));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        assertThat(captured.get()).isNull();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    // ── 인증 예외 경로(미인증 허용) + 스푸핑 차단 ──

    @Test
    @DisplayName("/api/v1/auth/** 는 토큰 없이 통과(Kakao 로그인 시작)")
    void auth_path_passes_without_token() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/kakao"));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        assertThat(captured.get()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    @DisplayName("인증 예외 경로라도 클라이언트가 위조한 신원 헤더는 제거")
    void auth_path_strips_spoofed_identity_headers() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/kakao")
                        .header(JwtAuthenticationFilter.HEADER_MEMBER_ID, "999")
                        .header(JwtAuthenticationFilter.HEADER_MEMBER_ROLE, "ADMIN"));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        HttpHeaders forwarded = captured.get().getRequest().getHeaders();
        assertThat(forwarded.containsKey(JwtAuthenticationFilter.HEADER_MEMBER_ID)).isFalse();
        assertThat(forwarded.containsKey(JwtAuthenticationFilter.HEADER_MEMBER_ROLE)).isFalse();
    }

    @Test
    @DisplayName("비-API 경로(/__fallback 등)는 인증 없이 통과")
    void non_api_path_passes_through() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/__fallback"));
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, capturing(captured)).block();

        assertThat(captured.get()).isNotNull();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }
}
