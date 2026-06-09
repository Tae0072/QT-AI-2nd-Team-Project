package com.qtai.bible;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qtai.common.dto.ApiResponse;
import com.qtai.common.exception.ErrorCode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 게이트웨이 미경유 직접 호출을 차단하는 deny-by-default 필터 (inbound 활성 시에만 등록).
 *
 * <p>인증(JWT 검증)은 게이트웨이가 수행하고, 신뢰 경계 진입을 두 방식으로 증명한다:
 * <ul>
 *   <li><b>공유 토큰 환경</b>({@code qtai.bible.gateway.shared-token} 설정 시): 게이트웨이가 모든 bible 라우트에
 *       주입하는 {@code X-Gateway-Token}이 일치하면 신뢰한다. 이 토큰은 ① 게이트웨이가 JWT 인증한 <b>사용자 호출</b>
 *       (신원 헤더 {@code X-Member-Id}/{@code X-Member-Role} 동반)과 ② 내부 <b>SYSTEM 서비스 호출</b>(배치/캐시 경계,
 *       사용자 컨텍스트 없음)을 모두 포함한다. bible는 읽기 전용 참조 데이터라 member 단위 인가가 없고, 신뢰 증명은
 *       토큰이다. 토큰 불일치/누락이면 401. (SYSTEM 호출은 {@code SYSTEM_BATCH} 주체로 취급한다.)</li>
 *   <li><b>토큰 미설정(dev)</b>: 공유 토큰이 없으므로 게이트웨이 주입 신원 헤더({@code X-Member-Id}+{@code X-Member-Role})를
 *       요구한다. SYSTEM 서비스 호출은 토큰 환경에서만 허용된다.</li>
 * </ul>
 * actuator(헬스체크)는 예외. full Spring Security 대신 경량 필터(starter-security 자동설정 미간섭).
 * 로그에 token 값을 남기지 않는다(CLAUDE.md §9).
 */
public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayHeaderAuthenticationFilter.class);
    // 감사 트레일 — SYSTEM 서비스-to-서비스 호출을 INFO로 남겨 별도 감사 appender로 라우팅 가능(CLAUDE.md §5).
    private static final Logger auditLog = LoggerFactory.getLogger("com.qtai.audit.bible");

    static final String HEADER_MEMBER_ID = "X-Member-Id";
    static final String HEADER_MEMBER_ROLE = "X-Member-Role";
    static final String HEADER_GATEWAY_TOKEN = "X-Gateway-Token";

    private final ObjectMapper objectMapper;
    private final String expectedGatewayToken; // 현재 토큰. null/blank이면 토큰 미설정(dev) — 신원 헤더 요구
    private final String previousGatewayToken; // 직전 토큰(회전 grace window). null/blank이면 미사용

    public GatewayHeaderAuthenticationFilter(ObjectMapper objectMapper, String expectedGatewayToken) {
        this(objectMapper, expectedGatewayToken, null);
    }

    /**
     * @param previousGatewayToken 직전 토큰(무중단 회전용 grace window). 현재값과 함께 한시적으로 허용하며,
     *                             모든 호출자가 새 토큰으로 전환되면 비운다. blank이면 현재값만 허용.
     */
    public GatewayHeaderAuthenticationFilter(
            ObjectMapper objectMapper, String expectedGatewayToken, String previousGatewayToken) {
        this.objectMapper = objectMapper;
        this.expectedGatewayToken = expectedGatewayToken;
        this.previousGatewayToken = previousGatewayToken;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        if (StringUtils.hasText(expectedGatewayToken)) {
            // 토큰 환경: 유효한 X-Gateway-Token = 신뢰(게이트웨이 사용자 전달 또는 내부 SYSTEM 서비스 호출).
            if (!tokenMatches(request.getHeader(HEADER_GATEWAY_TOKEN))) {
                writeUnauthorized(response);
                return;
            }
            // 주체 판정 기준(명확화): 토큰 유효 + 사용자 신원 헤더(X-Member-Id) 부재 = SYSTEM_BATCH 서비스 호출.
            //   X-Member-Id가 있으면 게이트웨이가 전달한 USER 호출(별도 감사 불필요).
            //   (role 부재 여부는 판정에 쓰지 않는다 — 사용자 식별자 X-Member-Id 유무가 단일 기준.)
            boolean systemCall = !StringUtils.hasText(request.getHeader(HEADER_MEMBER_ID));
            if (systemCall) {
                // 감사 트레일에 INFO로 기록(주체=SYSTEM_BATCH, token 값·PII 미기록).
                auditLog.info("SYSTEM_BATCH bible 서비스 호출 허용 method={} path={}", request.getMethod(), path);
            }
            chain.doFilter(request, response);
            return;
        }

        // 토큰 미설정(dev): 게이트웨이 주입 신원 헤더(2종) 필수. SYSTEM 호출은 토큰 환경에서만.
        String memberId = request.getHeader(HEADER_MEMBER_ID);
        String memberRole = request.getHeader(HEADER_MEMBER_ROLE);
        if (!StringUtils.hasText(memberId) || !StringUtils.hasText(memberRole)) {
            log.debug("인증 실패: 토큰 미설정 환경에서 신원 헤더 누락 path={}", path);
            writeUnauthorized(response);
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean tokenMatches(String provided) {
        if (!StringUtils.hasText(provided)) {
            return false;
        }
        // 현재값 또는 직전값(회전 grace window) 일치. 각각 상수시간 비교(타이밍 공격 방지).
        // 단락 평가 회피를 위해 두 비교를 모두 수행한 뒤 OR.
        boolean matchesCurrent = constantTimeEquals(provided, expectedGatewayToken);
        boolean matchesPrevious = StringUtils.hasText(previousGatewayToken)
                && constantTimeEquals(provided, previousGatewayToken);
        return matchesCurrent || matchesPrevious;
    }

    private static boolean constantTimeEquals(String provided, String expected) {
        if (!StringUtils.hasText(expected)) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8));
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        ApiResponse<Void> body = ApiResponse.error(
                ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage());
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
