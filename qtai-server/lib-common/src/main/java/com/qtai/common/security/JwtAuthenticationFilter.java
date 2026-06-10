package com.qtai.common.security;

import com.qtai.common.exception.ErrorCode;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
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
 * <p>각 서비스는 {@code Authorization: Bearer {token}}을 먼저 {@link JwtValidator}(RS256 공개키)로
 * 검증해 SecurityContext에 사용자 인증을 설정한다. 토큰이 없으면 통과(permitAll 경로 처리).
 *
 * <p>사용자(RS256) 검증이 실패하면, 시스템 토큰 검증기({@link SystemTokenValidator}, HS256 공유 시크릿)가
 * 등록돼 있을 때 <b>시스템 토큰으로 폴백</b> 검증한다. 배치·스케줄러(SYSTEM_BATCH) 호출은 전달할 사용자
 * JWT가 없으므로 단명 시스템 토큰을 쓴다. 시스템 토큰이 유효하면 {@code memberId=0} + {@code ROLE_SYSTEM_BATCH}로
 * 인증을 설정한다. <b>사용자·시스템 검증이 모두 실패해야 401</b>이다(RS256 사용자 경로 동작은 그대로 유지).
 *
 * <p>{@code security.jwt.public-key}가 설정된 경우에만 활성화된다.
 * 로그에 token 값·시크릿 절대 남기지 않는다 (CLAUDE.md §9).
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "security.jwt", name = "public-key")
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtValidator jwtValidator;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;
    /** 시스템 토큰 검증기 — security.jwt.system-secret 미설정 시 null(폴백 비활성). */
    private final SystemTokenValidator systemTokenValidator;

    public JwtAuthenticationFilter(JwtValidator jwtValidator,
                                   SecurityErrorResponseWriter securityErrorResponseWriter,
                                   ObjectProvider<SystemTokenValidator> systemTokenValidatorProvider) {
        this.jwtValidator = jwtValidator;
        this.securityErrorResponseWriter = securityErrorResponseWriter;
        this.systemTokenValidator = systemTokenValidatorProvider.getIfAvailable();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = extractToken(request);

        if (StringUtils.hasText(token)) {
            UsernamePasswordAuthenticationToken authentication = authenticate(token);
            if (authentication == null) {
                // 사용자(RS256)·시스템(HS256) 검증 모두 실패 → 401. 토큰 값은 로그에 남기지 않음 (CLAUDE.md §9)
                log.warn("JWT 검증 실패(사용자·시스템 모두) — uri={}", request.getRequestURI());
                SecurityContextHolder.clearContext();
                securityErrorResponseWriter.write(response, ErrorCode.UNAUTHORIZED);
                return;
            }
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 사용자(RS256) 검증을 먼저 시도하고, 실패하면 시스템 토큰(HS256)으로 폴백한다.
     *
     * @return 인증 토큰, 둘 다 실패하면 {@code null}
     */
    private UsernamePasswordAuthenticationToken authenticate(String token) {
        try {
            Long memberId = jwtValidator.validateAndGetMemberId(token);
            String role = jwtValidator.extractRole(token);
            return buildAuthentication(memberId, role);
        } catch (JwtException | IllegalArgumentException userTokenError) {
            // RS256 사용자 토큰 검증 실패 → 시스템 토큰 폴백(검증기가 등록된 경우에만)
            return authenticateSystemToken(token);
        }
    }

    private UsernamePasswordAuthenticationToken authenticateSystemToken(String token) {
        if (systemTokenValidator == null) {
            return null;
        }
        try {
            long systemMemberId = systemTokenValidator.validateAndGetSystemMemberId(token);
            return buildAuthentication(systemMemberId, systemTokenValidator.systemRole());
        } catch (JwtException | IllegalArgumentException systemTokenError) {
            return null;
        }
    }

    private UsernamePasswordAuthenticationToken buildAuthentication(Object principal, String role) {
        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
    }

    private String extractToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
