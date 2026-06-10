# 워크플로우 — admin-web 리스트 훅 통일 (useList 제거)

- 작성자: 강태오 (Lead)
- 일자: 2026-06-09
- 저장소: QT-AI-2nd-Team-Project (구현)
- 브랜치: `chore/admin-web-list-hook-unify` → PR 대상 `dev`

## 1. 배경

AD-04 신고처리 화면을 두 PR이 병렬로 구현했다.

- #374(김지민): `useList` 훅 + 토스트(AntApp) 기반
- #369(강태오): `usePagedList` 훅 기반

#374가 먼저 dev에 머지된 뒤 #369가 충돌했고, Lead 결정으로 #369의 AD-04(usePagedList)
버전으로 덮어썼다. 그 결과 `useList.ts`가 dev에 남았지만 이를 import하는 화면이 없어졌다.

## 2. 조사

- `git grep useList` 결과: 정의(`useList.ts`) 한 줄 외 참조 0건 → 죽은 코드 확정.
- `usePagedList` 사용처: `AuditLogsPage`, `AiAssetsPage`, `ReportsPage`.
- `main.tsx`의 AntApp(토스트/message 컨텍스트)은 `ReportsPage`가 `message.success/error`로
  실제 사용 중 → 유지(삭제 대상 아님).

## 3. 조치

- `admin-web/src/hooks/useList.ts` 삭제(미사용). 리스트 훅을 `usePagedList`로 단일화.
- `main.tsx` 토스트 설정·기타 #374 산출물은 그대로 유지.

## 4. 검증

- `npm run typecheck`(tsc --noEmit) 통과 — 삭제 후 미해결 참조 없음.
- `npm run build`(vite) 통과 — 3113 modules transformed, 빌드 성공.

## 5. 후속

- 향후 admin-web 신규 목록 화면은 `usePagedList`를 사용한다.
- 페이지네이션 응답 계약(content/page/size/totalElements...)은 v3.1 표준 유지.
