# Workflow - 2026-06-12 QT 본문 게시 상태 노출 게이트

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/admin-web-stabilization` |
| PR 대상 | `dev` |
| 관련 F-ID | AD-02, F-01 |
| 트리거 | 관리자 `오늘 QT 관리` QA 중 날짜가 다른 QT를 여러 개 게시할 수 있음을 확인했고, 게시/숨김 상태가 사용자 API 노출 게이트에 실제로 반영되는지 점검 |
| 기준 문서 | `04_API_명세서.md`, `qtai-server/admin-server/src/main/resources/db/migration/V31__add_qt_passage_admin_status.sql` |

## 작업 목표

`qt_passages.status`가 사용자 노출 정책에 반영되도록 서버 게이트를 보강한다. 날짜가 서로 다른 QT를 여러 개 `active`로 둘 수 있는 정책은 유지하되, `hidden` 또는 `pending_review` 상태의 본문은 사용자 오늘 QT, 사용자 ID 직접 조회, 성경 본문 해설 진입점, 내부 콘텐츠 컨텍스트의 `published` 플래그에서 공개 본문처럼 취급하지 않는다.

## 범위

- `GET /api/v1/qt/today`가 오늘 또는 fallback 날짜 본문을 조회할 때 `ACTIVE` 상태만 사용한다.
- `GET /api/v1/qt/passages/{id}`는 날짜가 지났더라도 `ACTIVE`가 아니면 404로 숨긴다.
- `GET /api/v1/qt/passage-study`는 숨김/대기 QT에 승인 해설이 있어도 해설 진입점을 노출하지 않는다.
- `GET /api/v1/qt/passages/{id}/content-context`와 날짜 기반 콘텐츠 컨텍스트는 내부 배치가 본문을 찾을 수는 있게 두되, `ACTIVE && 공개일 지남`일 때만 `published=true`로 반환한다.
- `admin-server`와 `service-bible`에 중복 존재하는 QT 조회 경로를 같은 규칙으로 맞춘다.

## 제외 범위

- 날짜가 서로 다른 QT를 여러 개 `active`로 둘 수 있는 정책 변경
- 관리자 화면명, 필터 UX, 목록 기본 필터 변경
- AI 해설 생성/검증 로직 변경
- 성경 본문 텍스트 seed 추가

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageLookup.java` | 오늘 QT 캐시 조회 시 `ACTIVE` 상태만 조회 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtService.java` | 사용자 ID 조회와 콘텐츠 컨텍스트 `published`에 게시 상태 반영 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageRepository.java` | 날짜 + 상태 조회 메서드 추가 |
| Modify | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtPassage.java` | `status`, `publishedAt`, `hiddenAt` 매핑 추가 |
| Create | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtPassageStatus.java` | service-bible의 QT 게시 상태 enum 정의 |
| Modify | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtPassageLookup.java` | 오늘 QT 캐시 조회 시 `ACTIVE` 상태만 조회 |
| Modify | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtService.java` | 사용자 ID 조회, passage-study, 콘텐츠 컨텍스트 노출 게이트 보강 |
| Modify | `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtPassageRepository.java` | 날짜 + 상태 조회 메서드 추가 |
| Test | `qtai-server/service-bible/src/test/java/com/qtai/domain/qt/internal/QtServiceTest.java` | hidden 본문 직접 조회, 콘텐츠 컨텍스트, passage-study 차단 검증 |
| Test | `qtai-server/service-bible/src/test/java/com/qtai/bible/QtContentContextApiTest.java` | 공개 시드 상태 보정 |
| Test | `qtai-server/service-bible/src/test/java/com/qtai/bible/QtPassageStudyApiTest.java` | hidden QT가 해설 진입점으로 노출되지 않음 검증 |
| Test | `qtai-server/service-bible/src/test/java/com/qtai/bible/QtVideoControllerTest.java` | 네이티브 SQL QT 시드에 게시 상태 명시 |
| Test | `qtai-server/service-bible/src/test/java/com/qtai/domain/qtvideo/internal/QtVideoClipPreparationEventIntegrationTest.java` | pending review import는 영상 준비를 스킵하도록 기대값 보정 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-12_qt-passage-publish-visibility-gate_report.md` | 원인, 수정 내용, 검증 결과 기록 |

## 구현 순서

1. 관리자 publish/hide API가 변경하는 `qt_passages.status`와 사용자 조회 경로의 불일치를 확인한다.
2. `service-bible`의 `QtPassage`에 게시 상태 컬럼 매핑과 상태 enum을 추가한다.
3. 양쪽 `QtPassageRepository`에 `findByQtDateAndStatus(LocalDate, QtPassageStatus)`를 추가한다.
4. 양쪽 `QtPassageLookup.findTodayPassage()`에서 `ACTIVE` 본문만 조회하도록 바꾼다.
5. 양쪽 `QtService.getPassage()`에서 `ACTIVE && 공개일 지남` 조건을 만족하지 않으면 404로 숨긴다.
6. `QtService.toContentContext()`의 `published` 계산을 날짜 단독 조건에서 `ACTIVE && 공개일 지남`으로 바꾼다.
7. `service-bible`의 `getPassageStudy()`에서 hidden/pending QT를 후보에서 제외한다.
8. 단위/통합 테스트와 네이티브 SQL 시드를 새 정책에 맞춘다.
9. 관련 Gradle 테스트와 컴파일을 실행하고 report를 작성한다.

## 수용 기준

- [x] 날짜가 서로 다른 QT는 여러 개 `active`가 될 수 있다.
- [x] `hidden` 오늘 QT는 사용자 `today` 조회 대상이 아니다.
- [x] `hidden` 또는 `pending_review` QT는 사용자 ID 직접 조회에서 404로 취급된다.
- [x] `hidden` QT는 승인 해설이 있어도 `passage-study` 진입점을 노출하지 않는다.
- [x] 내부 콘텐츠 컨텍스트는 본문을 반환하되 `hidden`/`pending_review`이면 `published=false`를 반환한다.
- [x] 기존 관리자 publish/hide API 계약은 유지된다.
- [x] 관련 Gradle 테스트가 통과한다.
- [x] report에 원인과 검증 결과가 남는다.

## 검증 계획

```powershell
.\gradlew.bat :service-bible:test --tests "com.qtai.domain.qt.internal.QtServiceTest"
.\gradlew.bat :service-bible:test --tests "com.qtai.bible.QtContentContextApiTest" --tests "com.qtai.bible.QtPassageStudyApiTest" --tests "com.qtai.bible.QtVideoControllerTest" --tests "com.qtai.domain.qtvideo.internal.QtVideoClipPreparationEventIntegrationTest"
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.qt.internal.AdminQtPassageServiceTest" :service-bible:compileJava :admin-server:compileJava
```

## 후속 QA 후보

- 관리자 화면명 `오늘 QT 관리`가 실제 정책과 맞는지 별도 UI QA에서 판단한다.
- 관리자 목록에 `오늘` 빠른 필터가 필요한지 별도 UX 항목으로 판단한다.
