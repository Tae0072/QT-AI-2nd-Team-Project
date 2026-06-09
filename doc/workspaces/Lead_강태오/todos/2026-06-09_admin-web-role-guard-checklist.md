# 2026-06-09 · admin-web D2 권한 가드 체크리스트

## D2 권한 가드

- [x] `GET /api/v1/admin/me` 호출 함수 추가
- [x] 로그인 상태에 관리자 본인 정보 연결
- [x] 토큰은 있지만 관리자 정보 조회 실패 시 로그인 해제
- [x] `SUPER_ADMIN`은 전체 화면 접근
- [x] `requiredRoles=[]` 화면은 활성 관리자 공통 접근
- [x] 사이드바 메뉴를 권한별로 필터링
- [x] 직접 URL 접근도 라우트에서 차단
- [x] 권한 부족 화면 표시
- [x] `typecheck` 통과
- [x] `build` 통과

## 후속 후보

- [ ] AD-05 찬양 큐레이션 로컬 QA
- [ ] `package-lock.json` 커밋 여부 결정
- [ ] F2 환경/배포 정리

## 검증 결과

- `npm.cmd run typecheck`: 통과
- `npm.cmd run build`: 통과
- 참고: Vite 500kB chunk size 경고는 기존 antd 번들 경고이며 비차단.
