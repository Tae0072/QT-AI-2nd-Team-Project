# 워크플로우 — 시뮬레이터 승인→게시 자동 배선 (AI 자산 리뷰)

근거: 관리자웹 조치 TODO(2026-06-15) "후속 B — 시뮬레이터 승인→게시 자동 배선". F-06/F-12.
대상: `qtai-server/admin-server` (실제 관리자 AI 자산 리뷰가 실행되는 모놀리식).

## 1. 배경 / 막혀 있던 이유

`AiAssetReviewService.reviewAiAsset`는 EXPLANATION+BIBLE_VERSE 승인 시에만 study로 게시(`publishCommandsForTarget`가 `isVerseExplanationBibleVerseAsset`만 통과)했고, SIMULATOR는 승인해도 `simulator_clips` 공개 클립이 생성/숨김되지 않았다. 게시 서비스(`SimulatorClipPublishService.publishApprovedSimulatorClip`/`hidePublishedSimulatorClip`)는 존재했지만 **승인 흐름에 호출처가 없었다(고아)**.

단순 미러링이 아니라 막혀 있던 진짜 이유: **SIMULATOR 자산의 payloadJson 스키마가 미정의**였다(생성은 `NOT_IMPLEMENTED`, 게시에 필요한 title·componentLibraryVersionId·sceneScript를 어디서 읽을지 계약 없음).

## 2. 결정 — SIMULATOR 자산 payload 계약 신규 정의 (Lead 승인 2026-06-15)

SIMULATOR + QT_PASSAGE 자산의 `payloadJson`은 다음 키를 가진다(향후 시스템 등록 경로가 이 계약을 따른다):

```json
{
  "title": "클립 제목(문자열)",
  "componentLibraryVersionId": 7,
  "sceneScript": { "...": "장면 스크립트 객체" }
}
```

게시 커맨드 매핑:

| PublishApprovedSimulatorClipCommand | 출처 |
| --- | --- |
| qtPassageId | `asset.getTargetId()` (SIMULATOR의 target은 QT_PASSAGE) |
| title | payload `title` |
| componentLibraryVersionId | payload `componentLibraryVersionId` |
| sceneScriptJson | payload `sceneScript` 를 다시 문자열화 |
| aiAssetId | `asset.getId()` |
| approvedAt | `command.reviewedAt()` |

## 3. 변경 내용 (admin-server)

`domain/ai/internal/AiAssetReviewService.java`
- study.api `PublishApprovedSimulatorClipUseCase`·`HidePublishedSimulatorClipUseCase` 주입(해설/용어와 동일 패턴).
- `approve()`: `simulatorCommandForTarget()`가 활성화+SIMULATOR/QT_PASSAGE일 때 게시 커맨드 생성 → `publishSimulatorClip()` 호출(실패 시 경고 로그 후 rethrow → 트랜잭션 롤백·재시도 시 재생).
- `hide()`: SIMULATOR/QT_PASSAGE면 `hidePublishedSimulatorClip(aiAssetId)` 호출.
- payload 검증: sceneScript 누락/Null, componentLibraryVersionId 누락/비정수, title 공백 시 `INVALID_INPUT`.

`domain/ai/internal/AiAssetReviewServiceTest.java`
- 생성자에 시뮬레이터 UseCase 2개 mock 추가.
- 신규 테스트 3: `approveSimulatorPublishesClip`(커맨드 필드 검증), `approveSimulatorWithoutActivateDoesNotPublish`, `hideSimulatorHidesClip`.

도메인 경계: ai.internal → study.api UseCase 호출(기존 해설 게시와 동일한 허용 의존). 새 import는 study.api 한정. ArchUnit/Modulith 경계 위반 없음.

## 4. 남은 후속 — service-ai 미러 (동기화 규칙)

CLAUDE.md 동기화 규칙상 도메인 로직 원본은 service-*다. 단, service-ai는 study 게시를 **RestClient 어댑터**로 호출하는데(예: `VerseExplanationRestClientAdapter`) **시뮬레이터용 어댑터가 없다**. 따라서 service-ai에 그대로 주입하면 빈 부재로 기동이 깨진다.

미러 시 필요한 작업(후속 PR):
1. service-ai에 `SimulatorClipPublishRestClientAdapter`(study 서비스 `POST .../simulator-clips/publish|hide` 호출) 신설 + `StudyPublishClient`에 메서드 추가 + Mock.
2. service-ai `AiAssetReviewService`에 동일 분기 적용.

이번 PR은 **실제 관리자 리뷰가 실행되는 admin-server에 한정**한다(런타임 동작 확보). service-ai 미러는 어댑터 신설을 동반하므로 분리한다.

## 5. 검증

```bash
cd qtai-server
.\gradlew.bat :admin-server:compileJava :admin-server:compileTestJava
.\gradlew.bat :admin-server:test --tests "com.qtai.domain.ai.internal.AiAssetReviewServiceTest"
```

런타임 한계: SIMULATOR 생성/등록 경로가 아직 없어 실제 자산으로의 E2E는 불가. 단위 테스트로 배선 로직(승인→게시 커맨드, 숨김→hide 호출)을 검증한다.
