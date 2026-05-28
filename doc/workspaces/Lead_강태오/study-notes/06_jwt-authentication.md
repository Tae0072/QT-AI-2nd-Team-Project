# JWT 토큰 인증 — 로그인 상태 유지

> **왜 배워야 하나:** QT-AI는 카카오 로그인 후 JWT 토큰으로 사용자 인증 상태를 유지한다. 노션에서 Spring Security와 Filter Chain을 배웠지만, JWT 토큰의 생성/검증/갱신 방식은 다루지 않았다.

---

## 1. JWT가 뭔가?

JWT(JSON Web Token)는 로그인한 사용자 정보를 **토큰(문자열)**에 담아서 주고받는 방식이다.

전통적인 세션 방식은 서버가 로그인 상태를 기억해야 했다. JWT는 **토큰 자체에 정보가 들어있어서** 서버가 상태를 저장하지 않아도 된다(Stateless).

## 2. JWT 구조

JWT는 점(.)으로 구분된 3개 파트로 이루어진다:

```
xxxxx.yyyyy.zzzzz
 헤더  페이로드  서명
```

```json
// 헤더 (Header) — "어떤 알고리즘으로 서명했나?"
{
  "alg": "HS256",
  "typ": "JWT"
}

// 페이로드 (Payload) — "누구의 토큰인가?"
{
  "sub": "12345",           // 회원 ID
  "role": "USER",           // 역할
  "iat": 1716883200,        // 발급 시각
  "exp": 1716969600         // 만료 시각
}

// 서명 (Signature) — "이 토큰이 위조되지 않았나?"
HMACSHA256(헤더 + 페이로드, 비밀키)
```

## 3. QT-AI 인증 흐름

```
1. 사용자가 Flutter 앱에서 카카오 로그인
2. 카카오 SDK가 카카오 토큰을 발급
3. Flutter 앱이 서버로 카카오 토큰 전달
   → POST /api/v1/auth/kakao { accessToken: "카카오토큰" }
4. 서버가 카카오 API로 사용자 정보 확인
5. 서버가 JWT Access Token + Refresh Token 발급
6. 이후 API 호출마다 헤더에 JWT 포함
   → Authorization: Bearer eyJhbGciOiJIUzI1...
```

## 4. Access Token vs Refresh Token

| 구분 | Access Token | Refresh Token |
|------|-------------|---------------|
| 용도 | API 호출 시 인증 | Access Token 재발급 |
| 유효 기간 | 짧음 (예: 30분) | 김 (예: 7일) |
| 저장 위치 | 앱 메모리 | Secure Storage |
| 탈취 위험 | 짧은 유효기간으로 완화 | 서버에서 무효화 가능 |

## 5. Spring Security + JWT 연동

```java
// JWT 필터 — 모든 요청에서 토큰을 확인
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) {
        // 1. 헤더에서 토큰 추출
        String token = resolveToken(request);

        // 2. 토큰 유효성 검증
        if (token != null && jwtProvider.validate(token)) {
            // 3. 토큰에서 사용자 정보 추출
            Authentication auth = jwtProvider.getAuthentication(token);
            // 4. Spring Security 컨텍스트에 저장
            SecurityContextHolder.getContext().setAuthentication(auth);
        }

        chain.doFilter(request, response);
    }
}
```

노션에서 배운 Filter Chain 위에 JWT 검증 필터를 추가하는 구조다.

## 6. 주의사항

- JWT 비밀키는 절대 코드에 직접 작성하지 않는다 → 환경 변수나 시크릿 관리 도구 사용
- Access Token 유효 기간은 짧게, Refresh Token은 서버에서 무효화할 수 있게 설계
- 로그에 토큰 값을 남기지 않는다 (CLAUDE.md §9 규칙)

## 7. 참고 자료

- JWT 공식 사이트: https://jwt.io/
- JWT 디코더 (학습용): https://jwt.io/#debugger
- Spring Security + JWT 가이드: https://www.baeldung.com/spring-security-oauth-jwt
