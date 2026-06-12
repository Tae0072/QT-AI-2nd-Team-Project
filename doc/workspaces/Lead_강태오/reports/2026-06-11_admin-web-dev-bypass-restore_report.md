# 리포트 — admin-web dev-bypass 재도입 (2026-06-11)

## 요약
PR #459 롤백으로 끊긴 admin-web 로컬 로그인을, dev-bypass 3파일 짝(`client.ts` + `env.ts` + `DevUserIdHeaderFilter`)을 원본 기준으로 재이식해 복구했다. 운영 경로에는 영향이 없도록 dev 전용 가드를 그대로 유지했다.

## 진단
- 막힘 고리: (롤백 후) 개발용 토큰 로그인 → `client.ts`가 가짜 토큰을 `Bearer`로 전송 + `X-Dev-*` 헤더 없음 → BE에서 인증주체 미설정/권한 부족 → `/admin/me` 401·403 → `AuthContext`가 토큰 삭제 → `/login` 회귀(무한 차단).
- 화면 파일(`LoginPage`/`AuthContext`/`ProtectedRoute`/`kakao.ts`)은 롤백 영향 없음. 끊긴 지점은 정확히 dev-bypass 헤더 주입(FE)과 역할 주입(BE) 두 곳이었다.

## 조치 (변경 3파일, 모두 dev 전용)
| 파일 | 변경 |
|---|---|
| `admin-web/src/config/env.ts` | `IS_DEV`, `DEV_ADMIN_MEMBER_ID`(기본 `'1'`) export 추가 |
| `admin-web/src/api/client.ts` | dev에서 `X-Dev-User-Id`/`X-Dev-Roles: ADMIN` 주입, 가짜 토큰 Bearer 미첨부 |
| `qtai-server/.../security/DevUserIdHeaderFilter.java` | `X-Dev-Roles` 파싱 `resolveAuthorities()` 복원 → `ADMIN`→`ROLE_ADMIN` |

원본 출처: `924654ef`(#345) + 백업 `5c6728d`(#509). 최소 diff로 재이식.

## 검증 결과
- 헤더 짝: FE 송신 ↔ BE 수신 헤더명/역할 매핑 일치 확인.
- import 짝: `client.ts` ↔ `env.ts` export 일치 확인.
- 토글: `application-dev.yml=true` / `application-prod.yml=false` 현행 유지.
- prod 안전: `IS_DEV=false` 시 헤더·개발 입력란 미노출, BE 필터/설정은 `@Profile("dev")`+toggle로 비활성, `prod` 프로파일이면 `DevSecurityConfig` 부트 차단.
- 정합성 검토 2~3회 완료(짝·문법·흐름·prod 안전).

## 검증 권장 명령(로컬에서 실행 권장)
- BE: `./gradlew -p qtai-server :admin-server:compileJava` (필터 컴파일), 필요 시 `:admin-server:test`.
- FE: `cd admin-web && npm run build` (tsc 타입체크 포함) 또는 `npm run dev` 후 로그인 동작 확인.
- 동작 확인: vite dev로 admin-web 기동 → 로그인 화면 "개발용 토큰" 입력란에 아무 값 입력 → 대시보드 진입 → 관리자 API 200 확인.
> 본 세션 샌드박스에서는 프로젝트 의존성·gradle 캐시 부재 및 git 인덱스 손상으로 빌드 실행은 보류, 정적 검토로 갈음.

## 대안(참고)
dev-bypass를 끈 채 정식 JWT로 기동: `./gradlew -p qtai-server :admin-server:bootRun --args='--spring.profiles.active=dev --qtai.security.dev-bypass=false'` 또는 `QTAI_SECURITY_DEV_BYPASS=false`. 이 경우 admin-web 로그인엔 실제 ADMIN JWT(카카오 키/토큰)가 필요.

## 후속(미수행)
- 브랜치 `fix/admin-web-dev-bypass-restore`로 분리 → PR 대상 `dev`. `dev`/`master` 직접 push 금지. 팀 MSA 중 PR 머지 타이밍은 Lead 판단.
- 동일 롤백이 재발하지 않도록, dev-bypass 3파일을 revert 단위에서 분리(별도 PR 유지)하는 정책 검토 권장.
