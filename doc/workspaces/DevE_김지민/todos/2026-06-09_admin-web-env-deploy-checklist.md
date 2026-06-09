# 2026-06-09 · admin-web F2 환경/배포 정리 체크리스트

## PR #419 후속

- [x] PR #419 종합 의견 확인
- [x] 머지된 `dev-admin-web`에 코드 변경과 `package-lock.json` 포함 여부 확인
- [x] Vite/esbuild dev dependency moderate 2건 후속 추적 항목화

## F2 환경/배포

- [x] README 실행 절차를 `npm ci` 기준으로 갱신
- [x] `VITE_API_BASE_URL`, `VITE_API_PROXY_TARGET` 사용 기준 문서화
- [x] Vite proxy와 배포 API base URL 차이 설명
- [x] 현재 구현 화면과 권한 표 갱신
- [x] `/admin/me` 권한 조회와 오류 처리 정책 문서화
- [x] `.env.example`에서 미지원 `VITE_DEV_ADMIN_MEMBER_ID` 제거
- [ ] 실제 ADMIN 토큰으로 브라우저에서 AD-05 목록/필터/등록 API 동작 확인

## 검증

- [x] `npm.cmd run typecheck`
- [x] `npm.cmd run build`
- [x] `npm.cmd audit --omit=dev`
- [x] `git diff --check`

## 후속

- 실제 운영/스테이징 배포 주소가 확정되면 배포 환경별 `.env` 값은 별도 배포 문서에 반영한다.
- Vite/esbuild dev dependency moderate 2건은 Vite 8 업그레이드 영향 검토 후 별도 PR로 처리한다.
