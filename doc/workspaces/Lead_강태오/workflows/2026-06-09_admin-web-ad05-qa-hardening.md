# 2026-06-09 · admin-web AD-05 QA + 권한 가드 보강 워크플로우

## 배경

PR #418 머지 후 Claude 자동 리뷰 종합 의견에서 후속 일감 3개가 확인됐다.

- ADMIN 토큰 기반 브라우저 검증과 AD-05 QA 결과 기록.
- `package-lock.json` 커밋 여부 결정.
- `/admin/me` 권한 조회 실패 시 401/403/네트워크 오류 분기 확인.

## 작업 순서

1. `dev-admin-web` 최신화 후 `feature/admin-web-ad05-qa-hardening` 분기.
2. `/admin/me` 실패 처리 보강: 인증/인가 실패만 로그아웃, 네트워크 오류는 재시도 UX.
3. `package-lock.json`을 의존성 고정 산출물로 커밋 대상에 포함.
4. AD-05 찬양 큐레이션 정적 QA: 금지 입력 필드(가사·음원·외부 URL) 부재 확인.
5. `typecheck`/`build` 검증 및 리포트 작성.

## 제외

- `qtai-server/**` 수정.
- 실제 찬양 수정/숨김 연결.
- AD-01/02/06 구현.
