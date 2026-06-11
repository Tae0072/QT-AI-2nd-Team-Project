package com.qtai.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * dev 프로파일 전용 임시 인증 필터.
 *
 * <p>{@code X-Dev-User-Id} 헤더로 memberId를 받아 SecurityContext에 인증 정보를 세팅한다.
 * 정식 카카오/JWT 흐름이 완성되기 전까지 Postman·통합테스트에서 사용자 ID를 주입하기 위한
 * 임시 우회 메커니즘.
 *
 * <p>운영 사고 방지 가드:
 * <ul>
 *   <li>{@code @Profile("dev")} — prod·default 프로파일에서는 빈 등록 자체가 안 됨</li>
 *   <li>{@code @ConditionalOnProperty} — dev-bypass 토글이 true일 때만 활성</li>
 * </ul>
 *
 * <p>정식 JWT 인증이 완성된 뒤에도 이 필터는 삭제하지 않는다. application-dev.yml의
 * {@code qtai.security.dev-bypass: false}로 비활성화하면 자동으로 작동 중지.
 */
@Slf4j
@Component
@Profile("dev")
@ConditionalOnProperty(name = "qtai.security.dev-bypass", havingValue = "true")
public class DevUserIdHeaderFilter extends OncePerRequestFilter {

    private static final String HEADER_NAME = "X-Dev-User-Id";
    private static final String ROLES_HEADER = "X-Dev-Roles";
    private static final String DEFAULT_ROLE = "ROLE_USER";

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HEADER_NAME);

        if (StringUtils.hasText(header)) {
            try {
                Long memberId = Long.parseLong(header.trim());
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                memberId,
                                null,
                                resolveAuthorities(request)
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException e) {
                log.warn("Invalid {} header — value={}, uri={}",
                        HEADER_NAME, header, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 부여할 권한 목록을 만든다. 기본은 {@code ROLE_USER}이며,
     * dev 전용 {@code X-Dev-Roles} 헤더(쉼표 구분, 예: {@code ADMIN})가 있으면
     * 각 값을 {@code ROLE_*} 권한으로 추가한다.
     *
     * <p>관리자 웹(admin-web) dev 로그인에서 {@code X-Dev-Roles: ADMIN}으로
     * {@code ROLE_ADMIN}을 주입해, 관리자 API의 1차 권한(ROLE_ADMIN) 검사를 통과시키는 용도.
     */
    private List<SimpleGrantedAuthority> resolveAuthorities(HttpServletRequest request) {
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(DEFAULT_ROLE));
        String rolesHeader = request.getHeader(ROLES_HEADER);
        if (StringUtils.hasText(rolesHeader)) {
            for (String raw : rolesHeader.split(",")) {
                String role = raw.trim().toUpperCase(Locale.ROOT);
                if (role.isEmpty()) {
                    continue;
                }
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                if (!authority.equals(DEFAULT_ROLE)
                        && authorities.stream().noneMatch(a -> a.getAuthority().equals(authority))) {
                    authorities.add(new SimpleGrantedAuthority(authority));
                }
            }
        }
        return authorities;
    }
}
