# 리포트 - 관리자 웹 QA·권한표 정합화 (2026-06-12)

## 요약
관리자 웹의 메뉴/라우트 권한표를 `origin/dev`의 실제 백엔드 인가와 비교했다. AD-01 대시보드가 프런트에서는 세부 역할 무관으로 열려 있었지만, 백엔드는 `OPERATOR` 또는 `REVIEWER`만 허용하므로 프런트 권한표를 좁혔다. README의 오래된 관리자 웹 현황도 현재 구현 상태에 맞게 갱신했다.

## 변경 내용
- `admin-web/src/constants/menu.ts`
  - AD-01 대시보드 권한을 `OPERATOR`, `REVIEWER`로 변경
  - AD-01~AD-10 권한 근거 주석을 현재 백엔드 구현 기준으로 정리
- `admin-web/src/constants/roles.ts`
  - `OPERATOR`, `REVIEWER`, `CONTENT_CREATOR` 설명을 현재 관리자 기능 기준으로 보강
- `admin-web/README.md`
  - 구현 완료 화면을 AD-01~AD-10 기준으로 갱신
  - AD-01~AD-10 화면/권한 표 갱신
  - Vite proxy 기본 포트를 admin-server `8090` 기준으로 갱신

## 로그인 WIP 보호
- 로그인 화면, 인증 API, 토큰 저장 로직은 수정하지 않았다.
- 기존 로그인 작업 폴더의 미추적 파일은 건드리지 않았다.

## 검증
- 통과: `npm run typecheck`
- 통과: `npm run build`
- 통과: `git diff --check`
- 참고: `npm run build`에서 Ant Design 번들 크기 경고가 출력되었으나 빌드는 성공했다. 이번 변경과 직접 관련된 오류는 아니다.

## 남은 리스크
- 실제 브라우저 클릭 QA는 관리자 계정과 admin-server 실행 상태가 필요하다.
