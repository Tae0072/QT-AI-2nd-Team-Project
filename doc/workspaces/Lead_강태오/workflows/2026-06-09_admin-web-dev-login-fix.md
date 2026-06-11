# 워크플로우 — admin-web dev 로그인 401 수정

- 작성자: 강태오 (Lead)
- 일자: 2026-06-09
- 대상 저장소: QT-AI-2nd-Team-Project (구현)
- 브랜치: `bugfix/admin-web-dev-login-401` → PR 대상 `dev`
- 관련 화면/기능: 관리자 웹(admin-web) dev 로그인, AD-01~08 공통 진입
- 관련 PR: 화면 구현 PR #369(별도), 본 인증 수정 PR

## 1. 배경 / 문제

dev 프로파일로 admin-web을 띄워 관리자 API(`/api/v1/admin/**`)를 호출하면 모든 요청이
`401 "인증이 필요합니다"`로 거절되었다. dev 우회 인증(`DevUserIdHeaderFilter`,
`X-Dev-User-Id` / `X-Dev-Roles`)이 이미 dev에 반영(#345)되어 있었음에도 인증이 풀리지 않았다.

## 2. 원인 분석

1. 헤더 전달 자체는 정상 — 디버그 로그로 `X-Dev-User-Id=1`, `X-Dev-Roles=ADMIN`이
   서버에 도달함을 확인했다.
2. 진짜 원인은 **admin-web이 가짜 Bearer 토큰(`Authorization: Bearer dev-bypass`)을 함께 보낸 것**.
   `JwtAuthenticationFilter`가 이 값을 실제 JWT로 파싱하려다 실패("JWT 검증 실패")하고
   인증 컨텍스트를 비워, dev 헤더로 세팅한 인증까지 무효화되었다.
3. 보조 원인 — axios v1에서 커스텀 헤더를 대괄호 대입(`config.headers['X-...'] = ...`)으로
   설정하면 요청에서 누락될 수 있다. `config.headers.set(...)`이 안전하다.

## 3. 조치

`admin-web/src/api/client.ts` 요청 인터셉터:

- dev 환경에서는 가짜 Bearer 토큰을 `Authorization` 헤더로 보내지 않는다
  (`if (token && !IS_DEV)`). dev 인증은 `X-Dev-*` 헤더가 전담한다.
- dev 헤더는 `config.headers.set('X-Dev-User-Id', ...)`, `set('X-Dev-Roles', 'ADMIN')`으로 설정.

운영(prod) 경로는 변경 없음 — 정식 카카오/JWT 흐름은 그대로 Bearer를 전송한다.

## 4. 검증

- 로컬 dev 서버 + admin-web 기동 후 AD-07/AD-04/AD-03/AD-08 화면 정상 응답 확인(401 해소).
- `npm run typecheck`(tsc --noEmit) / `npm run build`(vite) 통과.
- 게이트웨이 계약 유지: 단일 baseURL(`VITE_API_BASE_URL`) + `/api/v1/admin/**` 경로만 의존.

## 5. 게이트웨이 / MSA 정합성

admin-web은 단일 base URL 하나만 바라본다. 백엔드가 v2 MSA로 분리되어도 게이트웨이가
`/api/v1/admin/**` 경로·요청/응답 형태를 동일하게 유지하면 admin-web은 무수정으로 동작한다.
호스트 하드코딩 없음.

## 6. 후속

- 정식 카카오/JWT 로그인 화면 연동 시 dev 헤더 분기는 dev 프로파일 빌드에서만 활성.
- `DevUserIdHeaderFilter`는 `qtai.security.dev-bypass: false`로 언제든 비활성화 가능.
