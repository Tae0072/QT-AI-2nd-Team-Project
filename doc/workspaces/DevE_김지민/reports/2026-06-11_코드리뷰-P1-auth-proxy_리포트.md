# Report — 2026-06-11 코드리뷰 P1 admin-web 인증 프록시

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김지민 (admin-web FE) |
| 브랜치 | `fix/admin-web-auth-proxy` (base `origin/dev` `12776bd`) |
| PR 링크 | (push 후 작성) |
| 커밋 | `5ecf935` |
| 출처 TODO | `2026-06-10_코드리뷰_TODO_김지민.md` TODO 1 (P1) |
| 워크플로우 | [workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md](../workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md) |

## 변경 내용

관리자 카카오 로그인(`POST /api/v1/admin/auth/kakao`)이 service-user(8081)로 가도록 dev vite proxy에 구체 경로 룰 추가. 기존엔 모든 `/api`가 admin-server(8090)로 갔는데 8090엔 auth 컨트롤러가 없어(#452는 service-user 구현) 로그인이 실패했다.

- `admin-web/vite.config.ts`: `authProxyTarget`(`VITE_AUTH_PROXY_TARGET` ?? 8081) 추가, `'/api/v1/admin/auth'` 룰을 `'/api'`보다 **위에** 배치(http-proxy는 정의 순서대로 매칭).
- `admin-web/.env.example`: `VITE_AUTH_PROXY_TARGET=http://localhost:8081` 항목·설명 추가.
- `admin-web/src/pages/LoginPage.tsx`: "백엔드 준비 전 실로그인 불가" 주석을 "라우팅 연결 완료(#452), 카카오 JS 키만 주입하면 동작"으로 정정. (개발용 토큰 입력 블록 유지)

> dev 한정 분리이며 게이트웨이(결정③) 확정 시 두 target을 같은 주소로 두면 통합. 규모 3파일 +19/-2.

## 검증 결과

- `npm run typecheck` (tsc --noEmit) → 통과(에러 0).
- `npm run build` (tsc && vite build) → 통과(✓ built in 4.68s). 단일청크 1.15MB 경고 = P5c(code-split) 대상으로 확인, P1 범위 아님.
- 실로그인 E2E(service-user 8081 도달, 403/토큰발급)는 `VITE_KAKAO_JS_KEY`·이승욱 합동 필요 → push 후 검증.

## CI / 자동 리뷰 결과

- 자가 리뷰: Secret/금지패턴/도메인경계 위반 없음. admin-web 게이트(typecheck+build) 통과로 갈음(테스트 프레임워크 없음, SKILL 규칙6 보강).
- (push 후) GitHub Actions·Claude PR 리뷰 결과 추가 예정.

## 남은 리스크 / 조율

- ⚠️ **이승욱/강태오 조율**: vite 주석 결정③(admin-web=8090 단일·게이트웨이 라우팅)과 P1(auth 8081 분리)이 겹침. dev용 분리는 가역적이고 게이트웨이 뜨면 `.env`로 통합. "dev vite에 `/api/v1/admin/auth`→8081 분리룰 추가" 공유 후 PR(본문 명시). admin-server가 인증을 8081로 포워딩하는 방향이면 P1 대신 그 방식 채택.

## 다음 작업

- P1 push → dev 대상 PR(조율 멘트 본문 포함).
- 로드맵 다음: P3 `feat/admin-web-dashboard-dto`(AD-01) → qt-passages → notices → P5c → P5b → P4.
