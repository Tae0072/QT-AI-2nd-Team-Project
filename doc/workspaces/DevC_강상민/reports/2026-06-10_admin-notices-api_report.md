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

- PASS: `.\gradlew.bat :admin-server:build`
- BLOCKED: `npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml`
  - PowerShell `npx.ps1`은 실행 정책으로 차단되어 `npx.cmd`로 재시도했다.
  - `npx.cmd` 실행 결과 `.spectral.yaml` 파일이 worktree에 없어 ENOENT로 종료됐다.

## 비고

- `admin-web/**`는 수정하지 않았다.
- 생성/수정 응답은 별도 상세 GET이 없으므로 `body` 전체를 포함한 상세 DTO로 반환한다.
