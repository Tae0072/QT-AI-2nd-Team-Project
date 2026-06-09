# 2026-06-09 · admin-web AD-05 QA + 권한 가드 보강 체크리스트

## 리뷰 후속

- [x] PR #418 종합 의견 확인
- [x] `/admin/me` 401/403 실패는 세션 종료
- [x] `/admin/me` 네트워크/일시 오류는 세션 유지 + 재시도 안내
- [x] `package-lock.json` 커밋 포함
- [x] AD-05 가사 입력 필드 없음
- [x] AD-05 음원 파일/URL 입력 필드 없음
- [x] AD-05 직접 YouTube URL 입력 필드 없음
- [x] `typecheck` 통과
- [x] `build` 통과

## 후속

- [ ] 실제 ADMIN 토큰으로 브라우저에서 목록/필터/등록 API 동작 확인
- [ ] F2 환경/배포 정리

## 검증 결과

- `npm.cmd ci`: 통과. 실행 중이던 Vite dev server가 `esbuild.exe`를 잡고 있어 1차 실패했고, 해당 dev server 종료 후 재실행 통과.
- `npm.cmd audit --omit=dev`: 운영 의존성 취약점 0건.
- `npm.cmd audit`: dev dependency `vite/esbuild` moderate 2건. `npm audit fix --force`는 Vite 8 breaking change라 이번 안전 PR에서는 보류.
- `npm.cmd run typecheck`: 통과.
- `npm.cmd run build`: 통과. Vite chunk size 경고는 기존 antd 번들 경고로 비차단.
- AD-05 정적 QA: 등록 요청 타입과 Form 필드가 `title`, `artist`, `sourceType`, `licenseNote`, `status`뿐임을 확인.
