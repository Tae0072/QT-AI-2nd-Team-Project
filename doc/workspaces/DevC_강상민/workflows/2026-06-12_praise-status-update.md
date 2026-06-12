# Workflow - 2026-06-12 praise-status-update

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-05 / F-09 |
| 트리거 | 찬양 곡을 숨김/노출로 등록한 뒤 상태를 다시 바꿀 수 없어 운영 흐름이 막힘 |
| 기준 파일 | `admin-web/src/pages/PraiseSongsPage.tsx`, `admin-web/src/api/praiseSongs.ts`, `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/**` |

## 작업 목표

관리자 `찬양 큐레이션` 화면에서 기존 곡의 상태를 `ACTIVE` 또는 `HIDDEN`으로 다시 변경할 수 있게 한다. 등록 시 상태 선택만 있고 수정 시 상태 변경이 불가능하면 검수 후 공개, 공개 후 숨김 같은 운영 흐름을 처리할 수 없다.

## 범위

- 관리자 수정 API 요청 DTO에 선택적 `status` 필드를 추가한다.
- 관리자 도메인 수정 로직에서 `status`가 오면 상태를 갱신하고, 없으면 기존 상태를 유지한다.
- 관리자 웹 수정 모달에 상태 선택을 추가한다.
- 관련 서비스 테스트를 보강한다.
- 타입체크와 관련 테스트를 실행한다.
- 작업 리포트를 작성한다.

## 제외 범위

- 음원 업로드/재생 소스 연결
- 사용자 앱의 내 찬양 저장 구조 변경
- `sourceType=DEVICE` 관리자 등록 허용
- 숨김 전용 별도 엔드포인트 추가

## 구현 순서

1. `PraiseUpdateRequest`에 `status` 필드를 추가한다.
2. `PraiseSong.update(...)`가 상태 변경도 받을 수 있게 한다.
3. `PraiseService.update(...)`에서 상태 문자열을 검증/파싱한다.
4. `PraiseServiceTest`에 상태 변경 테스트와 기존 테스트 constructor 인자를 반영한다.
5. `admin-web` 수정 요청 타입과 수정 모달에 `status`를 연결한다.
6. `admin-web` typecheck와 `admin-server` 관련 테스트를 실행한다.
7. 브라우저에서 수정 모달 상태 변경을 확인한다.
8. report를 작성한다.

## 수용 기준

- 수정 모달에서 현재 상태가 표시된다.
- 수정 저장 시 `ACTIVE -> HIDDEN`, `HIDDEN -> ACTIVE` 변경이 가능하다.
- `status`가 없는 기존 수정 요청은 기존 상태를 유지한다.
- 잘못된 status는 저장되지 않는다.
- report가 작성된다.
