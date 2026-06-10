# 2026-06-10 admin-notices-api report

## 요약

AD-06 시스템 공지 API를 `admin-server`의 `notification` 도메인에 추가했다. 관리자 권한은 `ROLE_ADMIN`과 `VerifyAdminRoleUseCase.verifyAnyRole(memberId, ["OPERATOR"])`를 모두 요구하며, `SUPER_ADMIN`은 기존 우월권으로 통과한다.

## 변경 내용

- `notices` 테이블 migration 추가
- 공지 목록, 생성, 수정, 발행, 숨김 UseCase와 Controller 추가
- 활성 회원 ID 조회용 `member.api.ListActiveMemberIdsUseCase` 추가
- 공지 발행 시 활성 회원별 `notifications.type=NOTICE` 생성
- `NOTICE_CREATE`, `NOTICE_UPDATE`, `NOTICE_PUBLISH`, `NOTICE_HIDE` 감사 로그 기록
- 감사 로그 snapshot에서 `body` 원문 제외
- AD-06 OpenAPI paths/schemas 추가

## 테스트

- `NoticeServiceTest`
  - create DRAFT
  - update DRAFT only
  - publish DRAFT -> PUBLISHED + notificationResult
  - hide -> HIDDEN
  - invalid transition 409 대상 예외
  - body 원문 audit snapshot 미포함
- `AdminNoticeControllerTest`
  - OPERATOR/SUPER_ADMIN 200
  - REVIEWER/CONTENT_CREATOR/USER 403
  - 미인증 401
  - create 201, update/publish 200, hide 204

## 검증 결과

- PASS: `qtai-server` 디렉터리에서 `.\gradlew.bat :admin-server:build`
- BLOCKED: `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
  - PowerShell `npx.ps1`은 실행 정책으로 차단되어 `npx.cmd`로 재시도했다.
  - `npx.cmd` 실행 결과 `.spectral.yaml` 파일이 worktree에 없어 ENOENT로 종료됐다.

## 비고

- `admin-web/**`는 수정하지 않았다.
- 생성/수정 응답은 별도 상세 GET이 없으므로 `body` 전체를 포함한 상세 DTO로 반환한다.

## REQUEST_CHANGES 대응

- `publishNotice`의 상태 전이 트랜잭션과 알림 fan-out 트랜잭션을 분리했다.
- 회원별 알림 저장은 `REQUIRES_NEW` 트랜잭션으로 실행해 일부 실패가 publish 상태 전이를 rollback-only로 오염시키지 않도록 했다.
- 공지 제목/본문은 저장 전에 HTML 특수문자를 escape해 stored XSS 위험을 줄였다.
- eventKey 중복 skip, 일부 알림 생성 실패 카운트, HTML escape 테스트를 추가했다.
- `V30__create_notices.sql` migration이 diff에 포함되어 있음을 확인했다.

## REQUEST_CHANGES 2차 대응

- 저장 단계 HTML escape를 제거하고, 공지를 plain text 입력으로 제한해 `<`, `>` 문자를 거부하도록 변경했다.
- 수동 `TransactionTemplate`을 제거하고, publish 상태 전이와 알림 chunk 저장을 별도 Spring bean의 `@Transactional(REQUIRES_NEW)`로 분리했다.
- 회원별 직렬 트랜잭션 대신 최대 500명 단위 chunk fan-out으로 변경했다.
- 실제 Spring/H2 통합 테스트에서 알림 chunk rollback이 발생해도 `notices.status=PUBLISHED`가 유지되는지 검증했다.

## REQUEST_CHANGES 3차 대응

- 공지 감사 로그 기록을 `NoticeAuditWriter`의 `@Transactional(REQUIRES_NEW)` 경계로 명시했다.
- 감사 snapshot 수동 JSON 조립을 제거하고 Jackson `ObjectMapper` 기반 직렬화로 변경해 개행/탭 등 제어문자가 포함되어도 유효한 JSON이 되도록 했다.
- 알림 fan-out은 정상 경로에서 chunk 저장을 유지하고, chunk 실패 시에만 단건 재시도를 수행해 실패 범위를 row 단위로 좁혔다.
- 공지 목록 페이징 응답과 OpenAPI schema에 `sort=createdAt,desc`를 추가했다.
- `MemberService#listActiveMemberIds`의 `List` import를 정리했다.
