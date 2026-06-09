package com.qtai.gateway.filter;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.ErrorCode;
import com.qtai.common.security.AuthenticatedUser;
import com.qtai.common.security.JwtTokenVerifier;

import io.jsonwebtoken.JwtException;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * 게이트웨이 JWT 인증 필터 (WebFlux).
 *
 * <ul>
 *   <li>보호 대상은 {@code /api/v1/**}. 단 {@code /api/v1/auth/**}(Kakao 로그인 시작·토큰 재발급)는
 *       미인증 허용한다(CLAUDE.md §5: 미인증 사용자는 로그인 시작만 가능).</li>
 *   <li>{@code Authorization: Bearer <access token>}을 공개키로 검증({@link JwtTokenVerifier}).
 *       누락·만료·변조·refresh 타입·claim 누락이면 401 표준 envelope(ErrorCode.UNAUTHORIZED)로 차단.</li>
 *   <li>검증 성공 시 다운스트림에 {@code X-Member-Id}/{@code X-Member-Role}을 주입한다. 클라이언트가
 *       보낸 동일 헤더는 항상 제거해 신원 위조(spoofing)를 차단한다(게이트웨이만 신뢰 헤더 설정).</li>
 *   <li>로그에 token 값을 남기지 않는다(CLAUDE.md §9).</li>
 * </ul>
 */
@Component
public class JwtAuthenticationFilter implements WebFilter, Ordered {

    public static final String HEADER_MEMBER_ID = "X-Member-Id";
    public static final String HEADER_MEMBER_ROLE = "X-Member-Role";

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String PROTECTED_PREFIX = "/api/v1/";
    private static final String AUTH_PREFIX = "/api/v1/auth/";

    private final JwtTokenVerifier verifier;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenVerifier verifier, ObjectMapper objectMapper) {
        this.verifier = verifier;
        this.objectMapper = objectMapper;
    }

    @Override
    public int getOrder() {
        // 라우팅(다운스트림 전달) 이전에 인증을 수행한다.
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().value();

        // 보호 대상이 아니거나(게이트웨이 내부/비-API 경로) 인증 예외 경로면 통과.
        // 단, 위조된 신원 헤더는 어떤 경로에서도 통과시키지 않는다.
        if (!path.startsWith(PROTECTED_PREFIX) || path.startsWith(AUTH_PREFIX)) {
            return chain.filter(withStrippedIdentity(exchange));
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            return unauthorized(exchange);
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();

        AuthenticatedUser user;
        try {
            user = verifier.verifyAccessToken(token);
        } catch (JwtException | IllegalArgumentException e) {
            // 검증 실패 원인 메시지는 로깅하되 token 값은 남기지 않는다.
            return unauthorized(exchange);
        }

        ServerHttpRequest authenticated = request.mutate()
                .headers(headers -> {
                    headers.remove(HEADER_MEMBER_ID);
                    headers.remove(HEADER_MEMBER_ROLE);
                    headers.set(HEADER_MEMBER_ID, String.valueOf(user.memberId()));
                    headers.set(HEADER_MEMBER_ROLE, user.role());
                })
                .build();
        return chain.filter(exchange.mutate().request(authenticated).build());
    }

    /** 클라이언트가 위조해 보낼 수 있는 신원 헤더를 제거한 교환 객체를 만든다. */
    private ServerWebExchange withStrippedIdentity(ServerWebExchange exchange) {
        ServerHttpRequest stripped = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(HEADER_MEMBER_ID);
                    headers.remove(HEADER_MEMBER_ROLE);
                })
                .build();
        return exchange.mutate().request(stripped).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ApiResponse<Void> body = ApiResponse.error(
                ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException e) {
            bytes = ("{\"success\":false,\"data\":null,\"error\":{\"code\":\""
                    + ErrorCode.UNAUTHORIZED.getCode() + "\"}}").getBytes(StandardCharsets.UTF_8);
        }
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
