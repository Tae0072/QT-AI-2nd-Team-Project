package com.qtai.security;

/**
 * JWT 인증 필터.
 *
 * 모든 요청에서 Authorization: Bearer {token} 헤더를 읽어
 * JwtProvider 로 검증하고 SecurityContext 에 인증 정보를 설정한다.
 *
 * 허용 경로 (인증 불필요):
 *   POST /api/v1/auth/kakao   — 카카오 로그인 (CLAUDE.md §5: 서버사이드 /oauth2/** 경로 사용 안 함)
 *   POST /api/v1/auth/refresh — Access Token 재발급
 *
 * 토큰 만료 시 → 401 응답, 로그인 화면으로 이동 (Flutter 인터셉터 처리).
 */
// TODO: @Component @RequiredArgsConstructor
// TODO: extends OncePerRequestFilter
public class JwtAuthenticationFilter {

    // TODO: final JwtProvider jwtProvider;

    // TODO: @Override
    //        protected void doFilterInternal(HttpServletRequest request,
    //                                        HttpServletResponse response,
    //                                        FilterChain filterChain) {
    //            1) Authorization 헤더에서 Bearer 토큰 추출
    //            2) jwtProvider.validateAndGetMemberId(token) 호출
    //            3) UsernamePasswordAuthenticationToken 생성 → SecurityContext 에 설정
    //            4) 예외 시 401 응답 (로그에 token 값 남기지 않음)
    //        }
}
