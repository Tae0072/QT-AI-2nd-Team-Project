# Workflow - 2026-06-12 praise-create-status

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-05 / F-09 |
| 트리거 | 관리자 찬양 곡 등록 화면에서 `HIDDEN` 선택이 가능하지만 서버가 등록 시 항상 `ACTIVE`로 저장하는 문제 확인 |
| 기준 문서 | `admin-web/src/api/praiseSongs.ts`, `admin-web/src/pages/PraiseSongsPage.tsx`, `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/**`, `qtai-server/service-bible/src/main/java/com/qtai/domain/praise/**` |
| 해당 경로 | `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/**`, `qtai-server/service-bible/src/main/java/com/qtai/domain/praise/**`, `qtai-server/admin-server/src/test/java/com/qtai/domain/praise/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

관리자 찬양 곡 등록 시 `ACTIVE/HIDDEN` 선택값이 실제 `praise_songs.status`에 반영되도록 서버 계약을 보강한다. 운영자가 검수 전 곡을 `HIDDEN`으로 먼저 등록하고, 사용자 앱에는 `ACTIVE` 곡만 노출되게 하는 흐름을 명확히 한다.

`sourceType`은 관리자 큐레이션 등록 경로의 성격상 계속 `CURATED`로 고정한다. 가사, 음원, 외부 URL 저장은 계속 금지한다.

## 범위

- `PraiseCreateRequest`에 `status` 필드를 추가한다.
- `PraiseService.create()`에서 `request.status()`를 `PraiseSongStatus`로 파싱해 저장한다.
- status가 비어 있으면 기존 호환성을 위해 `ACTIVE`로 저장한다.
- 허용하지 않는 status는 `INVALID_INPUT`으로 거절한다.
- admin-server와 service-bible의 중복 praise 계약/서비스를 함께 반영한다.
- admin-server 서비스 테스트를 추가한다.
- 검증 후 report를 작성한다.

## 제외 범위

- `sourceType` 요청값 반영
- 가사, 음원, 외부 URL 저장
- 관리자 수정 화면에서 상태 변경 기능 추가
- 사용자 앱 화면 수정
- DB schema 변경

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/api/dto/PraiseCreateRequest.java` | 등록 요청 status 계약 추가 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/praise/internal/PraiseService.java` | status 파싱 및 저장 반영 |
| Modify | `qtai-server/service-bible/src/main/java/com/qtai/domain/praise/api/dto/PraiseCreateRequest.java` | 사용자 서비스 원본 로직 계약 동기화 |
| Modify | `qtai-server/service-bible/src/main/java/com/qtai/domain/praise/internal/PraiseService.java` | status 파싱 및 저장 반영 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/praise/internal/PraiseServiceTest.java` | `HIDDEN` 등록, 기본값, 잘못된 status 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_praise-create-status_report.md` | 변경 내용과 검증 결과 기록 |

## 구현 순서

1. admin-server `PraiseCreateRequest`에 `String status`를 추가한다.
2. admin-server `PraiseService.create()`에서 `parseCreateStatus` helper로 `ACTIVE/HIDDEN/null`을 처리한다.
3. service-bible의 동일 DTO/서비스에 같은 변경을 적용한다.
4. admin-server `PraiseServiceTest`에 `create` 검증 3건을 추가한다.
5. Gradle 테스트/컴파일과 admin-web typecheck를 실행한다.
6. 브라우저에서 `/praise-songs` 화면이 정상 렌더링되는지 확인한다.
7. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/praise/internal/PraiseServiceTest.java` | `HIDDEN` 요청이 HIDDEN으로 저장됨 |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/praise/internal/PraiseServiceTest.java` | status 미지정 시 ACTIVE 기본값 유지 |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/praise/internal/PraiseServiceTest.java` | 잘못된 status는 INVALID_INPUT |

## 수용 기준

- [ ] 등록 요청의 `status=HIDDEN`이 `praise_songs.status=HIDDEN`으로 저장된다.
- [ ] 등록 요청의 `status=ACTIVE` 또는 미지정은 ACTIVE로 저장된다.
- [ ] 잘못된 status는 400 계열 비즈니스 오류로 거절된다.
- [ ] 사용자 목록 `GET /api/v1/praise-songs`는 기존처럼 ACTIVE만 반환한다.
- [ ] 가사/음원/URL 저장 필드는 추가하지 않는다.
- [ ] 관련 테스트와 타입 검사가 통과한다.
- [ ] report가 작성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 praise 등록 계약과 서비스 로직에 집중되어 있다.
- admin-server와 service-bible의 동기화가 필요해 한 작업자가 같은 판단으로 맞추는 편이 충돌과 불일치를 줄인다.

### 위임 가능 작업

| Worker | 입력 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 서버 수정, 테스트 보강, 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.praise.internal.PraiseServiceTest"
.\gradlew.bat :admin-server:compileJava
.\gradlew.bat :service-bible:compileJava
npm.cmd --prefix admin-web run typecheck
```

브라우저 검증:

- `http://localhost:5173/praise-songs` 새로고침
- 등록 모달의 상태 선택이 유지되는지 확인
- 실제 등록 제출은 필요 시 사용자가 QA 데이터로 수행한다.

## 후속 작업으로 남길 항목

- 관리자 수정 화면에서 ACTIVE/HIDDEN 상태 변경 기능 추가 여부 결정
- 등록 모달에 “HIDDEN은 사용자 앱에 노출되지 않음” 안내 추가 검토
