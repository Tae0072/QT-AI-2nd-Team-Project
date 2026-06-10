# 2026-06-09 · admin-web AD-05 QA + 권한 가드 보강 리포트

## 요약

PR #418의 Claude 자동 리뷰 종합 의견을 반영했다. 관리자 권한 조회 실패 처리에서 네트워크 오류와 인증/인가 실패를 분리했고, `package-lock.json`을 npm 의존성 고정 산출물로 포함하기로 결정했다. AD-05 찬양 큐레이션은 금지 필드가 없는지 정적 QA를 수행했다.

## 변경 내용

| 영역 | 내용 |
|---|---|
| API 에러 | `ApiClientError` 추가. HTTP `status`와 서버 `code`를 보존 |
| 권한 조회 | `/admin/me` 401/403은 세션 종료, 네트워크/5xx/timeout은 세션 유지 |
| 보호 라우트 | 관리자 권한 확인 실패 화면과 `다시 시도` 버튼 추가 |
| 의존성 | `admin-web/package-lock.json` 커밋 포함 결정 |
| AD-05 QA | 등록 폼과 요청 타입에서 가사·음원·외부 URL 필드 부재 확인 |

## 검증

- `npm.cmd ci`: 통과
  - 1차 실행은 기존 Vite dev server가 `node_modules/@esbuild/win32-x64/esbuild.exe`를 점유해 실패
  - 해당 dev server 종료 후 재실행 통과
- `npm.cmd audit --omit=dev`: 운영 의존성 취약점 0건
- `npm.cmd audit`: dev dependency `vite/esbuild` moderate 2건
  - `npm audit fix --force`는 Vite 8 breaking change라 보류
- `npm.cmd run typecheck`: 통과
- `npm.cmd run build`: 통과
  - Vite chunk size 경고는 기존 antd 번들 경고로 비차단

## AD-05 QA 결과

- 등록 폼 필드: `title`, `artist`, `sourceType`, `licenseNote`, `status`
- 등록 요청 타입: `CreatePraiseSongRequest`
- 금지 필드 확인:
  - 가사 입력 필드 없음
  - 음원 파일/URL 입력 필드 없음
  - 직접 YouTube URL 입력 필드 없음

## 한계 / 후속

- 실제 ADMIN 토큰 기반 브라우저 QA는 아직 미완료. 목록/필터/등록 API 성공 여부는 백엔드와 유효 토큰이 준비된 상태에서 확인 필요.
- F2 환경/배포 정리는 다음 작업으로 유지.
- dev dependency 취약점은 Vite major upgrade 여부를 별도 작업으로 판단한다.
