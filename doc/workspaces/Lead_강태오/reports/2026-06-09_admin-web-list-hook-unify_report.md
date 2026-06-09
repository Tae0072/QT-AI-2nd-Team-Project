# 리포트 — admin-web 리스트 훅 통일 (useList 제거)

- 작성자: 강태오 (Lead)
- 일자: 2026-06-09
- PR 대상: `dev` / 브랜치: `chore/admin-web-list-hook-unify`
- 변경: `admin-web/src/hooks/useList.ts` 삭제 (+ workflow/report 문서)

## 요약

AD-04 덮어쓰기(Lead 결정) 이후 미사용으로 남은 `useList` 훅을 제거하고 목록 훅을
`usePagedList` 하나로 통일했다.

## 변경 내용

| 항목 | 변경 |
|---|---|
| `hooks/useList.ts` | 삭제 (import 0건, 죽은 코드) |
| `hooks/usePagedList.ts` | 유지 — 표준 목록 훅 |
| `main.tsx` AntApp 토스트 | 유지 — ReportsPage가 message로 사용 |

## 검증 결과

- typecheck(tsc --noEmit): 통과
- build(vite): 통과 (3113 modules, 4.2s)
- 미해결 참조/깨진 import 없음

## 리스크 / 후속

- 동작 영향 없음(미사용 파일 제거).
- 신규 목록 화면은 usePagedList 사용을 기본으로 한다.
