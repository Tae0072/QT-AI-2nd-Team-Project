# 2026-06-09 · admin-web F2 환경/배포 정리 워크플로우

## 목적

`dev-msa` 백엔드 분리 흐름과 충돌하지 않도록 `admin-web/` 및 개인 작업 문서만 수정한다.
PR #419 종합 의견에서 확인된 package-lock 포함 여부, dev dependency 취약점 추적 필요성, 다음 F2 환경/배포 정리를 반영한다.

## 작업 순서

1. `dev-admin-web` 최신 머지 상태 확인
2. `feature/admin-web-env-deploy-docs` 브랜치 생성
3. README의 오래된 골격 단계 설명을 최신 구현/실행/배포 가이드로 교체
4. `.env.example`에서 실제 코드가 사용하지 않는 dev-bypass 변수 설명 제거
5. Vite/esbuild dev dependency moderate 2건을 후속 추적 항목으로 명시
6. `typecheck`, `build`, 운영 의존성 audit로 문서성 변경의 비회귀 확인

## 범위

- 포함: `admin-web/README.md`, `admin-web/.env.example`, DevE 작업 문서
- 제외: 백엔드 코드, MSA 모듈, 관리자 API 계약 변경, 실제 ADMIN 토큰 브라우저 QA

## 판단 메모

- `VITE_DEV_ADMIN_MEMBER_ID`는 `.env.example`에만 있고 프런트 코드에서 읽지 않는다.
- 실제로 `X-Dev-User-Id`를 전송하지 않으므로 예시 변수로 남기면 인증 방식 오해가 생긴다.
- 이번 PR에서는 코드를 새로 추가하지 않고 문서와 예시 파일을 현재 구현에 맞춘다.
