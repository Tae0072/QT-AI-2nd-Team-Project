# Report - 2026-06-12 QT 본문 게시 상태 노출 게이트

## 요약

관리자 `/qt-passages` 화면에서 날짜가 다른 QT를 여러 개 게시할 수 있는 것은 현재 정책상 허용되는 동작이다. 다만 서버 사용자 조회 경로가 `qt_passages.status`를 보지 않고 날짜만으로 공개 여부를 판단하고 있어, `hidden` 또는 `pending_review` 상태가 사용자 API와 일부 내부 공개 플래그에 반영되지 않는 문제가 있었다.

이번 작업에서는 날짜 정책은 유지하고, 게시 상태가 사용자 노출 게이트에 반영되도록 서버 로직과 테스트를 보강했다.

## 확인한 원인

- 관리자 publish/hide API는 `qt_passages.status`, `published_at`, `hidden_at`을 변경한다.
- 사용자 오늘 QT 조회는 `findByQtDate(...)`만 사용해 `ACTIVE` 상태를 필터링하지 않았다.
- 사용자 ID 직접 조회는 미래 날짜만 막고 `HIDDEN`, `PENDING_REVIEW` 상태는 막지 않았다.
- 내부 콘텐츠 컨텍스트의 `published` 플래그가 날짜만 보고 계산됐다.
- 성경 본문 해설 진입점 `GET /api/v1/qt/passage-study`는 범위 매칭 QT를 찾은 뒤 게시 상태를 확인하지 않았다.
- `service-bible`의 `QtPassage` 엔티티에는 admin DB migration으로 추가된 게시 상태 컬럼 매핑이 없었다.

## 변경 내용

- `admin-server`와 `service-bible`의 오늘 QT 조회를 `findByQtDateAndStatus(..., ACTIVE)` 기준으로 변경했다.
- 사용자 ID 직접 조회와 콘텐츠 컨텍스트 `published` 계산에 `ACTIVE && qtDate <= today` 조건을 적용했다.
- `service-bible`의 `QtPassage`에 `status`, `publishedAt`, `hiddenAt` 매핑과 `QtPassageStatus` enum을 추가했다.
- `GET /api/v1/qt/passage-study`가 hidden/pending QT를 해설 진입점 후보에서 제외하도록 변경했다.
- 테스트 시드에서 공개 본문은 명시적으로 `publish()` 또는 `status='ACTIVE'`를 사용하도록 보정했다.
- 자동 import 후 `PENDING_REVIEW` 상태인 QT는 영상 준비 이벤트에서 스킵되는 기대값으로 조정했다.

## 변경 파일

- `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageLookup.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtPassageRepository.java`
- `qtai-server/admin-server/src/main/java/com/qtai/domain/qt/internal/QtService.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtPassage.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtPassageStatus.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtPassageLookup.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtPassageRepository.java`
- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtService.java`
- `qtai-server/service-bible/src/test/java/com/qtai/domain/qt/internal/QtServiceTest.java`
- `qtai-server/service-bible/src/test/java/com/qtai/bible/QtContentContextApiTest.java`
- `qtai-server/service-bible/src/test/java/com/qtai/bible/QtPassageStudyApiTest.java`
- `qtai-server/service-bible/src/test/java/com/qtai/bible/QtVideoControllerTest.java`
- `qtai-server/service-bible/src/test/java/com/qtai/domain/qtvideo/internal/QtVideoClipPreparationEventIntegrationTest.java`
- `doc/workspaces/DevC_강상민/workflows/2026-06-12_qt-passage-publish-visibility-gate.md`
- `doc/workspaces/DevC_강상민/reports/2026-06-12_qt-passage-publish-visibility-gate_report.md`

## 검증 결과

```powershell
.\gradlew.bat :service-bible:test --tests "com.qtai.domain.qt.internal.QtServiceTest"
```

결과: 성공

```powershell
.\gradlew.bat :service-bible:test --tests "com.qtai.bible.QtContentContextApiTest" --tests "com.qtai.bible.QtPassageStudyApiTest" --tests "com.qtai.bible.QtVideoControllerTest" --tests "com.qtai.domain.qtvideo.internal.QtVideoClipPreparationEventIntegrationTest"
```

결과: 성공

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.qt.internal.AdminQtPassageServiceTest" :service-bible:compileJava :admin-server:compileJava
```

결과: 성공

## 결론

`게시를 3개 다 할 수 있음`은 날짜가 다른 QT라면 허용되는 현재 정책이다. 문제는 게시 상태가 사용자 노출 게이트에 반영되지 않던 부분이었고, 이번 수정으로 `ACTIVE` 상태만 사용자에게 공개되도록 서버 게이트를 맞췄다.
