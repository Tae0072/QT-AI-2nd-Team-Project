# Report — 2026-06-11 코드리뷰 P3a admin-web 대시보드 DTO

| 항목 | 내용 |
| --- | --- |
| 담당자 | 김지민 (admin-web FE) |
| 브랜치 | `feature/admin-web-dashboard-dto` (base `origin/dev`) |
| PR 링크 | (PR 생성 후 작성) |
| 커밋 | `9ebf149e`(feat) · `6bd0385a`(docs) · `1e69a4bb`(폭 통일) + origin/dev 머지 |
| 출처 TODO | `2026-06-10_코드리뷰_TODO_김지민.md` TODO 3 (P3, AD-01) |
| 관련 F-ID | F-06 |
| 워크플로우 | [workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md](../workflows/2026-06-11_코드리뷰-TODO-admin-web-워크플로우.md) |

## 변경 내용

AD-01 대시보드의 임시 타입 `[key:string]:unknown`을 백엔드 `AdminDashboardResponse`(admin-server, #449) 1:1 미러 타입으로 교체하고, 제네릭 `Object.entries` 렌더를 목적성 렌더(렌더 A)로 전환.

- `src/api/dashboard.ts`: `DashboardSummary`/`TodayQt`/`TodayQtStatus`/`RecentAuditLog` 실타입(camelCase, nullable·enum 포함).
- `src/pages/DashboardPage.tsx` (렌더 A):
  - 카운트 3종(AI 검증 대기·신고 접수·신고 검토) = `Statistic` 카드(반응형 폭 `xs=24 / sm=8` 통일).
  - 오늘 QT = `Descriptions`(날짜·상태 `Tag`·제목·시뮬레이터·해설·캐시), `MISSING` 처리.
  - 최근 관리자 활동 = `Table`(시각·행위자·액션·대상) + 빈 상태 `Empty`.
  - "백엔드 준비 중" Alert 철거 → 실패 시 **에러 Alert + 재시도 버튼**.
- `formatDateTime` 유틸 재사용. 타입 출처: 계약서 `DevC_강상민/reports/2026-06-10_admin-dashboard-api_report.md`.

## 검증 결과

- `npm run typecheck` (tsc --noEmit) → 통과(에러 0).
- `npm run build` → 통과(✓ ~4s). 단일청크 1.15MB 경고 = P5c(code-split) 대상, 본 범위 아님.
- **실데이터 E2E 미수행** — admin-server(8090)·service-user가 로컬에 안 떠 있고(8090/30090/k8s 전부 다운), 관리자 인증(카카오 로그인 + admin_users 등록)이 갖춰지지 않아 라이브 확인 불가. 타입이 백엔드 DTO record와 1:1 일치함을 코드로 대조해 위험은 낮음.

## CI / 자동 리뷰 결과

- 자가 리뷰: 금지패턴·도메인경계·secret 위반 없음. admin-web 게이트(typecheck+build) 통과로 갈음.
- 브랜치명 `feat/` → **`feature/`로 정정**(CI branch-name 규칙: feature/bugfix/hotfix/chore/release/docs/test만 허용).
- (PR 생성 후) GitHub Actions·Claude PR 리뷰 결과 추가 예정.

## 남은 리스크 / 후속

- 실데이터 스모크는 백엔드 풀스택이 뜨는 환경(공용 dev 또는 admin-server 기동 + 관리자 토큰)에서 후속. 현재 로컬엔 admin 인증 경로 미비(회고/제안거리).
- 로드맵 다음: P3b `feature/admin-web-qt-passages-dto`(AD-02) → P3c notices → P5c → P5b → P4.

## 메모: 로컬 admin 인증 공백 (제안)

admin-web 관리자 화면을 로컬에서 검증할 길이 없음 — dev-bypass(`X-Dev-User-Id`)는 ROLE_USER만 부여, dev 시드는 일반 회원만 생성, 관리자 화면은 실 ADMIN JWT(카카오) + `admin_users` 등록 필요. service-user에 **dev 전용 admin 토큰 발급**(프로파일 가드) 또는 **공용 dev 서버**를 두는 안을 이승욱/강태오에 제안할 만함.
