# Workflow — 2026-06-10 admin-qt-passages-api

| 항목 | 내용 |
| --- | --- |
| 담당자 | 이지윤 |
| 브랜치 | `feature/admin-qt-passages-api` |
| PR 대상 | `dev` |
| 관련 F-ID | F-06 / AD-02 |
| 트리거 | 2026-06-10 팀원별 TODO PDF의 이지윤 항목: admin-server에 qt-passages API 신설 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, `AGENTS.md`, `CODE_CONVENTION.md` |
| 담당 경로 | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/**`, `qtai-server/admin-server/src/test/java/com/qtai/domain/qt/**`, `qtai-server/admin-server/src/main/resources/db/migration/**` |

## 작업 목표

관리자 웹이 `admin-server` 8090 기준으로 호출할 수 있는 AD-02 오늘 QT 관리 API를 추가한다. API는 목록, 등록, 수정, 게시, 숨김을 제공하고, 관리자 권한은 `ROLE_ADMIN` 1차 검증과 `admin_users.admin_role`의 `OPERATOR` 또는 `SUPER_ADMIN` 2차 검증을 모두 통과해야 한다.

현재 `qt_passages` 실제 스키마에는 문서의 `status`와 발행 시각 컬럼이 없으므로, admin-server 마이그레이션으로 모더레이션 상태 `active/hidden/pending_review/deletion_notified/removed`와 `published_at`, `hidden_at`을 추가한다. 사용자 앱 노출 정책과 성경 본문 저장 금지 정책은 건드리지 않는다.

## 범위

- `GET /api/v1/admin/qt-passages`: 상태, 날짜 범위, 검색어, 페이지 조건으로 QT 본문 목록 조회
- `POST /api/v1/admin/qt-passages`: QT 본문 등록
- `PATCH /api/v1/admin/qt-passages/{id}`: QT 본문 범위와 제목 수정
- `POST /api/v1/admin/qt-passages/{id}/publish`: QT 본문 게시
- `POST /api/v1/admin/qt-passages/{id}/hide`: QT 본문 숨김
- 등록, 수정, 게시, 숨김 감사 로그 기록
- 요청 DTO validation, 중복 날짜 및 잘못된 절 범위 방어
- 단위 또는 slice 테스트로 핵심 성공/실패 경로 검증

## 제외 범위

- 관리자 웹 화면 연동과 카카오 JS SDK 로그인 구현
- dashboard, notices 등 다른 관리자 API 구현
- 사용자 Flutter 앱 API 응답 변경
- 성경 본문 seed, fixture, response 데이터 추가
- service-bible 또는 gateway 라우팅 변경
- 김지민과 최종 응답 계약 합의가 필요한 필드명 확정 작업

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/admin-server/src/main/resources/db/migration/V31__add_qt_passage_admin_status.sql` | AD-02 상태/발행/숨김 컬럼 추가 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassage.java` | 상태 전이와 admin 수정 메서드 추가 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageRepository.java` | 날짜 중복 검증 조회 보강 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/api/admin/**` | 관리자 QT UseCase와 DTO 계약 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/AdminQtPassageService.java` | 목록/등록/수정/게시/숨김 비즈니스 로직과 audit 기록 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/web/AdminQtPassageController.java` | `/api/v1/admin/qt-passages` HTTP 엔드포인트 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/qt/internal/AdminQtPassageServiceTest.java` | 상태 전이, 중복 날짜, 감사 로그 검증 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/qt/web/AdminQtPassageControllerTest.java` | 요청 검증과 관리자 권한 검증 흐름 |

## 구현 순서

1. `QtPassageStatus` enum과 `qt_passages.status/published_at/hidden_at` 마이그레이션을 추가한다.
2. `QtPassage`에 `createDraft`, `updateAdminRange`, `publish`, `hide`, `status` 접근자를 추가한다.
3. 관리자 API 전용 UseCase/DTO를 `domain.qt.api.admin` 아래에 정의한다.
4. `AdminQtPassageService`에서 목록 조회, 날짜 중복, 범위 검증, 상태 전이, audit log 기록을 구현한다.
5. `AdminQtPassageController`에서 요청 DTO validation과 `ADMIN + OPERATOR/SUPER_ADMIN` 검증 후 UseCase를 호출한다.
6. 서비스 테스트와 컨트롤러 테스트를 추가해 성공 경로와 대표 실패 경로를 검증한다.
7. admin-server 범위 테스트와 빌드를 실행하고 결과를 report에 남긴다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `AdminQtPassageServiceTest.java` | 등록 시 `pending_review` 생성, 중복 `qtDate` 거부, 수정 시 날짜 중복 거부, publish/hide 상태와 audit 기록 |
| `AdminQtPassageControllerTest.java` | 목록/등록/수정/게시/숨김 mapping, validation 400, `ROLE_ADMIN` 없는 사용자 403, admin_users 권한 부족 403 |
| `AdminControllerSurfaceTest.java` | 새 컨트롤러가 `/api/v1/admin/**` 하위로 유지되는지 기존 테스트로 확인 |

## 수용 기준

- [ ] `GET /api/v1/admin/qt-passages`가 `status`, `from`, `to`, `q`, `page`, `size`를 처리한다.
- [ ] `POST /api/v1/admin/qt-passages`가 새 QT 본문을 `pending_review`로 등록한다.
- [ ] `PATCH /api/v1/admin/qt-passages/{id}`가 날짜/범위/제목을 수정하고 중복 날짜를 차단한다.
- [ ] `POST /api/v1/admin/qt-passages/{id}/publish`가 상태를 `active`로 바꾸고 `publishedAt`을 기록한다.
- [ ] `POST /api/v1/admin/qt-passages/{id}/hide`가 상태를 `hidden`으로 바꾸고 `hiddenAt`을 기록한다.
- [ ] 모든 AD-02 엔드포인트가 `ROLE_ADMIN`과 `admin_users.admin_role=OPERATOR` 또는 `SUPER_ADMIN`을 요구한다.
- [ ] 쓰기 작업은 audit log를 남긴다.
- [ ] 개역개정, ESV, NIV 본문 seed/test/fixture/response 데이터를 새로 추가하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 스키마, 엔티티, 서비스, 컨트롤러, 테스트가 같은 도메인 상태 전이에 강하게 연결되어 순차 확인이 안전하다.
- admin 권한과 audit 기록이 함께 얽혀 있어 한 에이전트가 코드 흐름을 끝까지 잡는 편이 재작업을 줄인다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 테스트, report 작성을 직접 수행한다.

## 검증 계획

- `./gradlew.bat -p qtai-server :admin-server:test --tests "*AdminQtPassage*"`
- `./gradlew.bat -p qtai-server :admin-server:test --tests "*AdminControllerSurfaceTest"`
- `./gradlew.bat -p qtai-server :admin-server:build`
- `git diff --check`

## 후속 작업으로 남길 항목

- 김지민과 admin-web 연동용 최종 JSON 필드명과 날짜 필터 UX 계약 합의
- 필요 시 OpenAPI admin-server 분리 문서에 AD-02 상세 스키마 반영
- dev-msa 통합 게이트 이후 admin-web base URL 8090 정합 검증
