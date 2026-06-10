# Workflow 2026-06-10 admin-notices-api

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-notices-api` |
| PR 대상 | `dev-msa` |
| 관련 F-ID | AD-06 |
| 트리거 | 관리자 시스템 공지 API 구현 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md` |
| 대상 경로 | `qtai-server/admin-server`, `qtai-server/apis/api-v1/openapi.yaml` |

## 작업 목표

관리자 웹이 시스템 공지를 작성, 수정, 발행, 숨김 처리할 수 있도록 AD-06 API를 `admin-server`에 추가한다. 공지 소유권은 `notification` 도메인에 두고, 관리자 권한은 `ROLE_ADMIN` 1차 검증과 `VerifyAdminRoleUseCase.verifyAnyRole(memberId, ["OPERATOR"])` 2차 검증을 모두 통과해야 한다.

## 범위

- `GET/POST/PATCH/POST publish/POST hide /api/v1/admin/notices` 구현
- `notices` migration 추가
- 발행 시 활성 회원 전체에 `notifications.type=NOTICE` 생성
- 공지 감사 로그 `NOTICE_CREATE`, `NOTICE_UPDATE`, `NOTICE_PUBLISH`, `NOTICE_HIDE` 기록
- OpenAPI 갱신 및 테스트 추가

## 제외 범위

- `admin-web/**` 수정
- 사용자 앱 공지 상세/숨김 안내 API 확장
- FCM, push provider, Redis 기반 발행 큐

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `domain/notification/api/**` | AD-06 UseCase와 응답 DTO |
| Create | `domain/notification/internal/Notice*` | 공지 엔티티, 상태 전이, 서비스, repository |
| Create | `domain/notification/web/AdminNoticeController.java` | HTTP API와 관리자 권한 검증 |
| Modify | `domain/member/api`, `domain/member/internal` | 활성 회원 ID 조회 포트와 구현 |
| Create | `db/migration/V30__create_notices.sql` | `notices` 테이블 |
| Test | `domain/notification/**` | 권한, 상태 전이, 감사 로그 snapshot 테스트 |
| Modify | `apis/api-v1/openapi.yaml` | AD-06 계약 추가 |

## 구현 순서

1. 별도 worktree `C:\workspace\msateam\QT-AI-admin-notices`에서 `origin/dev-msa` 기준 브랜치를 생성한다.
2. `notices` 엔티티와 repository, migration을 추가한다.
3. `member.api.ListActiveMemberIdsUseCase`를 추가하고 `MemberService`가 구현한다.
4. `NoticeService`가 목록, 생성, 수정, 발행, 숨김 상태 전이를 처리한다.
5. 발행 시 `NOTICE:{noticeId}:{memberId}` eventKey로 알림 중복 생성을 막고 결과 카운트를 반환한다.
6. 감사 로그 snapshot에는 `id/title/status/notificationResult`만 저장하고 `body` 원문은 제외한다.
7. Controller 테스트와 Service 테스트를 추가한다.
8. OpenAPI와 report를 갱신한 뒤 검증 명령을 실행한다.

## 테스트 보강 목록

| 테스트 파일 | 검증 |
| --- | --- |
| `NoticeServiceTest` | create DRAFT, DRAFT only update, publish, hide, invalid transition, body audit 미포함 |
| `AdminNoticeControllerTest` | OPERATOR/SUPER_ADMIN 허용, REVIEWER/CONTENT_CREATOR/USER/미인증 거부, create/update/publish/hide 상태 코드 |

## 수용 기준

- [ ] AD-06 다섯 API가 `ApiResponse` 계약과 상태 코드를 만족한다.
- [ ] `PUBLISHED/HIDDEN` 수정과 잘못된 publish는 `INVALID_STATUS_TRANSITION`으로 실패한다.
- [ ] 발행 알림 실패는 공지 발행을 되돌리지 않고 `notificationResult.failedCount`로 노출된다.
- [ ] 감사 로그 snapshot에 공지 본문 원문이 저장되지 않는다.
- [ ] `admin-web/**` 변경이 없다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 구현, 테스트, OpenAPI가 같은 DTO와 상태 전이 정책을 공유해 순차 확인이 안전하다.
- edit path가 `notification/member/openapi/doc`으로 이어져 통합 컨텍스트를 한 작업자가 유지하는 편이 충돌 위험이 낮다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 전체 구현과 검증을 직접 수행한다.

## 검증 계획

- `.\gradlew.bat :admin-server:build`
- `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
- `git status --short`로 `admin-web/**` 미수정 확인

## 후속 작업으로 남길 항목

- 사용자 앱 공지 상세 조회와 숨김 공지 링크 안내 정책은 별도 사용자 API 작업에서 확정한다.
- publish 상태 전이 직후 프로세스 중단으로 알림 fan-out이 시작되지 못한 공지를 재처리하는 배치/운영 도구를 별도 작업으로 검토한다.
- 활성 회원 수 증가에 대비해 `ListActiveMemberIdsUseCase`의 키셋 페이지/스트리밍 조회 전환을 별도 작업으로 검토한다.
