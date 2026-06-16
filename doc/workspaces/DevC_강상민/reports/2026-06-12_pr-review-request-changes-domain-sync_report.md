# Report - 2026-06-12 PR 리뷰 REQUEST_CHANGES 도메인 동기화 보강

## 요약

Claude 자동 코드 리뷰(v3.1)의 추가 `REQUEST_CHANGES` 항목을 코드 기준으로 재검증하고, 실제 회귀 위험이 있는 지점은 테스트와 구현으로 보강했다.

## 반영 내용

### 1. QT 자동 수입 기본 상태 보정

- `qtai-server/service-bible/src/main/java/com/qtai/domain/qt/internal/QtPassage.java`
- `qtai-server/service-bible/src/test/java/com/qtai/domain/qtvideo/internal/QtVideoClipPreparationEventIntegrationTest.java`

`service-bible`의 QT 자동 수입 경로는 00:00 공개/04:00 배치 정책을 유지해야 하므로 새 QT 본문 기본 상태를 `ACTIVE`로 되돌렸다. 비디오 클립 준비 이벤트 테스트도 자동 수입 후 `ACTIVE` 상태와 클립 생성이 유지되는지 검증하도록 수정했다.

관리자 수동 등록 경로인 `admin-server`의 `QtPassage.create`는 기존처럼 `PENDING_REVIEW`를 유지한다. 자동 수입과 관리자 등록의 운영 의미가 다르기 때문이다.

### 2. Praise create 상태 파싱 테스트 보강

- `qtai-server/service-bible/src/test/java/com/qtai/domain/praise/internal/PraiseServiceTest.java`

`service-bible`의 `PraiseService.create`에 대해 다음 테스트를 추가했다.

- `status=HIDDEN`이면 숨김 상태로 저장
- `status=null`이면 기본 `ACTIVE`
- 잘못된 status는 `INVALID_INPUT`

리뷰의 update-status 동기화 지적은 코드 기준으로 관리자 전용 기능이다. 현재 주석과 구조상 `service-bible` 동기화 대상은 사용자 create/list/save/listMy이며, 관리자 update/delete/listAdmin은 `admin-server` 소유로 유지한다.

### 3. Today QT 캐시 무효화 테스트 추가

- `qtai-server/admin-server/src/test/java/com/qtai/domain/qt/internal/AdminQtPassageServiceTest.java`

`update`, `publish`, `hide`가 모두 `todayQt` 캐시를 `allEntries=true`로 무효화하는지 어노테이션 회귀 테스트를 추가했다.

### 4. AI 산출물 상세 active job 매핑 테스트 추가

- `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AdminAiAssetQueryServiceTest.java`

상세 조회 응답에서 기존 생성 job과 별도로 동일 대상의 `QUEUED/RUNNING` active generation job이 `activeGenerationJob`으로 매핑되는지 서비스 테스트를 추가했다.

### 5. 마이그레이션 지적 재검증

- Commentary 마이그레이션 `V33__create_ai_commentary_materials.sql`은 이미 `origin/dev`에 존재한다.
- 이번 PR diff에는 AI asset 조회 성능 인덱스 `V37__add_ai_asset_list_performance_indexes.sql`이 포함되어 있다.

따라서 마이그레이션 누락 지적은 현재 브랜치 기준 실제 차단 항목이 아니라 리뷰 diff 범위 오해로 판단했다.

## 검증 명령

```powershell
.\gradlew.bat :service-bible:test --tests "com.qtai.domain.praise.internal.PraiseServiceTest" --tests "com.qtai.domain.qtvideo.internal.QtVideoClipPreparationEventIntegrationTest" :admin-server:test --tests "com.qtai.domain.qt.internal.AdminQtPassageServiceTest" --tests "com.qtai.domain.ai.internal.AdminAiAssetQueryServiceTest"
```

결과: 성공

```powershell
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.ai.internal.CommentaryMaterialServiceTest" --tests "com.qtai.domain.ai.internal.AdminAiAssetQueryRepositoryTest" --tests "com.qtai.domain.ai.internal.AdminAiAssetQueryServiceTest" --tests "com.qtai.domain.qt.internal.AdminQtPassageServiceTest" :service-ai:test --tests "com.qtai.domain.ai.internal.CommentaryMaterialServiceTest" :service-bible:test --tests "com.qtai.domain.praise.internal.PraiseServiceTest" --tests "com.qtai.domain.qtvideo.internal.QtVideoClipPreparationEventIntegrationTest" :service-bible:compileJava :service-ai:compileJava :admin-server:compileJava
```

결과: 성공

```powershell
git diff --check
```

결과: 성공. CRLF 변환 경고만 표시됨.

## 남은 판단 사항

PR 규모가 큰 문제는 여전히 남아 있다. 다만 사용자가 한 브랜치에서 QA 수정과 보고서를 반복하는 흐름을 선택했기 때문에 이번 대응에서는 구조 분할 대신 차단 항목 보강과 근거 문서화에 집중했다.
