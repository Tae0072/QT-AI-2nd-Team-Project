package com.qtai.common.security;

import com.qtai.common.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * MSA 공통 JWT 인증 필터 (검증 전용).
 *
 * <p>각 서비스는 {@code Authorization: Bearer {token}}을 {@link JwtValidator}(공개키)로 검증해
 * SecurityContext에 인증을 설정한다. 토큰이 없으면 통과(permitAll 경로 처리), 변조/만료면 401.
 *
 * <p>{@code security.jwt.public-key}가 설정된 경우에만 활성화된다.
 * 로그에 token 값 절대 남기지 않는다 (CLAUDE.md §9).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "security.jwt", name = "public-key")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtValidator jwtValidator;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    public JwtAuthenticationFilter(JwtValidator jwtValidator,
                                   SecurityErrorResponseWriter securityErrorResponseWriter) {
        this.jwtValidator = jwtValidator;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Long memberId = jwtValidator.validateAndGetMemberId(token);
                String role = jwtValidator.extractRole(token);

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                memberId,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException e) {
                // 토큰 값은 로그에 남기지 않음 (CLAUDE.md §9)
                log.warn("JWT 검증 실패 — uri={}, error={}", request.getRequestURI(), e.getMessage());
                SecurityContextHolder.clearContext();
                securityErrorResponseWriter.write(response, ErrorCode.UNAUTHORIZED);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
