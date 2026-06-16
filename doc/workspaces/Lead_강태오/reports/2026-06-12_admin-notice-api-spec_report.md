# 리포트 - 관리자 공지 API 명세 보강 (2026-06-12)

## 요약
PR #540에서 구현된 관리자 공지 상세 조회 API가 문서에 한 줄만 반영되어 있어, 자동 리뷰가 지적한 명세 완성도 부족을 보강했다. 실제 구현과 테스트 기준에 맞춰 응답 예시, 입력 검증, 상태 전이, 실패 코드를 명시했다.

## 변경 내용
- `AD-06 시스템 공지` 상단 기능 표에 상세 조회, 수정, 숨김 경로까지 반영
- `GET /api/v1/admin/notices/{id}` 상세 응답 예시 추가
- 공지 상세 조회가 `bodyPreview`가 아닌 전체 `body`를 반환한다는 목적 명시
- 입력 검증 규칙 추가
  - `title` 1~100자
  - `body` 1~10,000자
  - `<`, `>` 문자 금지
  - 생성 시 `status`는 생략 또는 `DRAFT`
  - 수정 요청에는 `status` 미포함
- 상태 전이 규칙 추가
  - 수정/발행은 `DRAFT`에서만 가능
  - 숨김은 이미 `HIDDEN`인 공지를 제외하고 가능
- 실패 코드에 구현 기준 코드 명시
  - `AD0003 ADMIN_ROLE_INSUFFICIENT`
  - `C0004 RESOURCE_NOT_FOUND`
  - `C0007 INVALID_STATUS_TRANSITION`
- 전체 API 요약 표에 `GET /api/v1/admin/notices/{id}` 행 추가 및 번호 재정렬
- 변경 이력 `v1.9` 추가

## 로그인 WIP 보호
- 로그인 화면, 인증 API, 토큰 저장, Vite 인증 프록시 파일은 수정하지 않았다.
- 별도 워크트리 `C:\workspace\QT-AI-admin-notice-api-spec`에서 진행했다.

## 검증
- `git diff --check` 통과
- `04_API_명세서.md` 상단 공통 규칙 확인: 각 JSON 예시는 별도 명시가 없으면 공통 envelope의 `data` 객체만 표시한다.
- `apis/*/openapi.yaml` 파일 없음 확인
- 문서 전용 변경이라 Java/TypeScript 빌드는 생략한다.

## 남은 리스크
- 없음. OpenAPI YAML이 추가되는 시점에는 같은 계약을 별도 스키마로 옮겨야 한다.
