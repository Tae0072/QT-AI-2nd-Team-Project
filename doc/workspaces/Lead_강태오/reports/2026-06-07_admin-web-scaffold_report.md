# 2026-06-07 · 관리자 웹(admin-web) 골격 — 리포트

작성: Claude (Lead 강태오/T 지시) · 대상 저장소: `QT-AI-2nd-Team-Project` (구현)
관련 워크플로우: `workflows/2026-06-07_admin-web-scaffold.md`

## 1. 결과 요약

관리자 전용 웹(`admin-web/`)의 전체 구조 골격을 생성하고 빌드 검증까지 완료했다.
기술 스택은 React + Vite + TypeScript + Ant Design으로 확정(Lead 승인). AD-01~08 화면은 빈 골격 상태이며 다음 단계에서 실제 구현한다.
**git 커밋은 워킹트리 비정상 상태로 보류**했다(아래 §4).

## 2. 산출물

- 신규 폴더 `admin-web/` (총 38개 파일, `node_modules`/`dist` 제외)
  - 빌드 설정 9 + API 11 + 인증/권한 5 + 레이아웃·라우팅 3 + 화면 11(공통 1 포함) + README 1 등
- 문서 2: 본 리포트, 워크플로우

## 3. 검증 결과 (2~3회 점검)

| 점검 | 방법 | 결과 |
|---|---|---|
| 의존성 설치 | `npm install` | 성공 (161 packages, ~25s) |
| 타입 검사 | `tsc --noEmit` | 통과 (오류 0) |
| 프로덕션 빌드 | `vite build` | 성공 (1486 modules, `dist` 생성) |
| 파일 오염 | null 바이트 전수 검사 | 오염 0 |
| 구조 정리 | 미사용 `tsconfig.node.json` 삭제 | 완료 |

빌드 경고: 단일 청크가 500kB 초과(antd 포함). 골격 단계에선 정상이며, 추후 코드 분할(dynamic import)로 개선 가능.

> 검증은 사용자 폴더(`D:\...\admin-web`)를 더럽히지 않도록 sandbox 임시 경로(`/tmp/admin-web-verify`)에 복사해 수행했다. 따라서 사용자 폴더에는 `node_modules`/`dist`가 생성되지 않았다(설치는 사용자가 직접 `npm install`).

## 4. 중요: git 상태 (조치 필요)

현재 구현 저장소 워킹트리는 비정상 상태다.

- 체크아웃 브랜치: `feature/music-background-play` — **커밋이 하나도 없는(unborn) 상태**.
- 워킹트리: 프로젝트 전체 파일이 `A`(staged)로 잡혀 있음(배경음악 등 진행 중 작업 포함 추정).
- `admin-web/`는 `??`(untracked).
- `dev`(957a99f)보다 `origin/dev`(c640d6b)가 2커밋 앞섬(#313 웹지원, #315 배경음악).

이 상태에서 임의로 `git commit`/브랜치 전환을 하면 T의 다른 작업이 섞이거나 유실될 수 있어 **자동 커밋을 하지 않았다.**

### 권장 처리 절차 (T 확인 후)

1. 현재 staged 내용이 무엇인지 먼저 확인: `git status`
2. 진행 중 작업과 분리가 필요하면 정리(예: 의도한 브랜치로 이동/스태시) 후,
3. `dev` 최신화: `git checkout dev && git pull`
4. 작업 브랜치 생성: `git checkout -b feat/admin-web-scaffold`
5. admin-web만 추가: `git add admin-web doc/workspaces/Lead_강태오/...2026-06-07_admin-web-scaffold*`
6. 커밋: `feat(admin-web): 관리자 웹 전체 구조 골격 (React+Vite+TS+antd, AD-01~08)`
7. push 후 `dev` 대상 PR.

> 메모: 이 환경의 git/커밋은 Windows MinGit 경로 이슈가 있었으므로, 커밋은 T가 직접 수행하는 편이 안전하다.

## 5. 남은 작업 / TODO

- [ ] git: 위 절차로 `feat/admin-web-scaffold` 브랜치 커밋·PR (T)
- [ ] 화면별 실제 구현(표/폼/모달/페이지네이션)
- [ ] 세부 권한별 메뉴 노출·접근 제어(`menu.ts` requiredRoles 최종 확정 — `04` 권한표 대조)
- [ ] 카카오 웹 로그인 / 서버 OAuth 연동
- [ ] `03_아키텍처_정의서.md` 기술 스택 확정 반영(문서 저장소 PR)

## 6. 규칙 준수 확인

- 관리자 화면을 `flutter-app/`이 아닌 `admin-web/`에 분리 ✔
- `/api/v1/admin/**` 호출 전제, 별도 백엔드 미생성 ✔
- AD-03 승인 전 원문 미노출, AD-05 음원·가사 미저장 규칙을 코드 주석/README에 명시 ✔
- 입문자 기준 한국어 주석·README 작성 ✔
