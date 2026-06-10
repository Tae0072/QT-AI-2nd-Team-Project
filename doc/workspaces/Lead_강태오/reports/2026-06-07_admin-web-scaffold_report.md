# 2026-06-07 · 관리자 웹(admin-web) 골격 — 리포트

작성: Claude (Lead 강태오/T 지시) · 대상 저장소: `QT-AI-2nd-Team-Project` (구현)
관련 워크플로우: `workflows/2026-06-07_admin-web-scaffold.md`

## 1. 결과 요약

관리자 전용 웹(`admin-web/`)의 전체 구조 골격을 생성하고 빌드 검증까지 완료했다.
기술 스택은 React + Vite + TypeScript + Ant Design으로 확정(Lead 승인). AD-01~08 화면은 빈 골격 상태이며 다음 단계에서 실제 구현한다.
admin-web만 분리해 `feature/admin-web-scaffold` 브랜치로 커밋·PR했다(T의 다른 작업은 미포함).

## 2. 산출물

- 신규 폴더 `admin-web/` (약 40개 파일, `node_modules`/`dist` 제외)
  - 빌드 설정 + API 11 + 인증/권한 5 + 레이아웃·라우팅 3 + 화면 11(공통 1 포함) + README 1
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

> 검증은 사용자 폴더를 더럽히지 않도록 sandbox 임시 경로(`/tmp/admin-web-verify`)에 복사해 수행했다. 따라서 사용자 폴더에는 `node_modules`/`dist`가 생성되지 않았다(설치는 사용자가 직접 `npm install`).

## 4. Git 처리

- 작업 시작 시 워킹트리에 T의 미커밋 작업(웹 실행 지원 코드 수정 등)이 섞여 있어, `git stash`로 보관 후 분리 진행.
- `origin/dev` 기반 `feature/admin-web-scaffold` 브랜치 생성 → `admin-web/` + 본 문서 2개만 `git add` → 커밋 → push → `dev` 대상 PR 생성.
- 작업 후 원래 브랜치로 복귀하여 `git stash pop`으로 T의 작업 상태 원복.
- 커밋: `feat(admin-web): 관리자 웹 전체 구조 골격 (React+Vite+TS+antd, AD-01~08)`

## 5. 남은 작업 / TODO

- [ ] 화면별 실제 구현(표/폼/모달/페이지네이션)
- [ ] 세부 권한별 메뉴 노출·접근 제어(`menu.ts` requiredRoles 최종 확정 — `04` 권한표 대조)
- [ ] 카카오 웹 로그인 / 서버 OAuth 연동
- [ ] `03_아키텍처_정의서.md` 기술 스택 확정 반영(문서 저장소 PR)

## 6. 규칙 준수 확인

- 관리자 화면을 `flutter-app/`이 아닌 `admin-web/`에 분리 ✔
- `/api/v1/admin/**` 호출 전제, 별도 백엔드 미생성 ✔
- AD-03 승인 전 원문 미노출, AD-05 음원·가사 미저장 규칙을 코드 주석/README에 명시 ✔
- 입문자 기준 한국어 주석·README 작성 ✔
