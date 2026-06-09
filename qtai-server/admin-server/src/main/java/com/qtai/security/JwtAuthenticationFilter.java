package com.qtai.security;

import com.qtai.common.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * JWT 인증 필터.
 *
 * <p>모든 요청에서 {@code Authorization: Bearer {token}} 헤더를 추출해
 * {@link JwtProvider}로 검증하고 {@link org.springframework.security.core.context.SecurityContext}에
 * 인증 정보를 설정한다.
 *
 * <p>허용 경로는 {@link SecurityConfig}의 permitAll에서 관리하며,
 * 이 필터는 토큰이 없으면 인증을 설정하지 않고 그냥 통과시킨다.
 * permitAll 경로가 아닌데 인증이 없으면 Spring Security가 401을 반환한다.
 *
 * <p>로그에 token 값 절대 남기지 않는다 (CLAUDE.md §9).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            try {
                Long memberId = jwtProvider.validateAndGetMemberId(token);
                String role = jwtProvider.extractRole(token);

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
