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
import java.util.List;

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
                                List.of(new SimpleGrantedAuthority(DEFAULT_ROLE))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException e) {
                log.warn("Invalid {} header — value={}, uri={}",
                        HEADER_NAME, header, request.getRequestURI());
            }
        }

        filterChain.doFilter(request, response);
    }
}
