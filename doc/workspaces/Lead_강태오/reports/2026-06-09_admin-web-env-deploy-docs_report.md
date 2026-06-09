# 2026-06-09 · admin-web F2 환경/배포 정리 리포트

## 요약

`dev-admin-web`에 PR #419가 머지된 뒤, 종합 의견을 반영해 환경/배포 문서를 최신 구현 상태에 맞췄다.
코드 동작 변경 없이 README, `.env.example`, 작업 문서만 정리했다.

## 변경 내용

| 구분 | 내용 |
|---|---|
| README | 실행, 환경 변수, 명령어, 폴더 구조, 화면/권한, 배포 메모를 최신화 |
| 환경 예시 | 실제 프런트 코드가 사용하지 않는 `VITE_DEV_ADMIN_MEMBER_ID` 제거 |
| 리뷰 후속 | Vite/esbuild dev dependency moderate 2건을 후속 추적 대상으로 명시 |
| 범위 관리 | 백엔드/MSA 파일은 수정하지 않음 |

## 확인한 기준

- `GET /api/v1/admin/me` 응답(`adminUserId`, `memberId`, `adminRole`)으로 메뉴/라우트 권한을 제한한다.
- 로컬 개발은 기본 `/api/v1` + Vite proxy 조합을 사용한다.
- MSA 게이트웨이를 경유할 때는 `VITE_API_PROXY_TARGET`만 게이트웨이 주소로 바꾸면 된다.
- 배포 시 proxy가 없다면 `VITE_API_BASE_URL`에 전체 API 주소를 넣어 빌드한다.

## 남은 일

- 실제 ADMIN 토큰으로 브라우저에서 AD-05 목록/필터/등록 API 동작 확인
- Vite/esbuild dev dependency moderate 2건은 Vite 8 업그레이드 영향 검토 후 별도 처리

## 검증 결과

- `npm.cmd run typecheck`: 통과
- `npm.cmd run build`: 통과. Vite chunk size 경고는 기존 antd 번들 경고로 비차단
- `npm.cmd audit --omit=dev`: 운영 의존성 취약점 0건
- `git diff --check`: 통과. Windows CRLF 변환 경고만 표시
