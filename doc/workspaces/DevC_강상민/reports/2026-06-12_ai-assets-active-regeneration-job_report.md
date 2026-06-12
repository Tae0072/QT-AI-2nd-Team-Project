# Report - 2026-06-12 AI 산출물 active 재생성 job 표시

## 요약

관리자 `AI 산출물 검증` 상세에서 이미 같은 대상의 재생성 job이 진행 중이어도 처음에는 `재생성` 버튼이 보이고, 버튼을 한 번 눌러 서버 오류를 받은 뒤에야 `재생성 작업 진행 중` 표시로 바뀌는 문제가 있었다.

원인은 프런트엔드가 클릭 후 받은 job만 로컬 상태로 기억하고, 서버 상세 응답은 산출물을 만든 원래 `generationJob`만 내려주던 구조였다. 같은 대상의 active 재생성 job을 상세 응답에 추가하고, admin-web이 그 값을 사용하도록 수정했다.

## 확인한 근거

- `AiAssetsPage.tsx`의 `activeRegenerationJob`은 `regenerationJobsByAssetId[selectedAsset.id]`만 보고 있었다.
- `regenerationJobsByAssetId`는 재생성 요청 성공 또는 중복 오류 catch 이후에만 채워졌다.
- 서버 `AdminAiAssetQueryRepository.findDetail()`은 `asset.generationJobId`로 join한 원래 생성 job만 조회했다.
- 서버 재생성 로직은 같은 `jobType`, `targetType`, `targetId`의 `QUEUED/RUNNING` job 존재 여부로 중복 재생성을 막고 있었다.

## 변경 내용

- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/api/admin/asset/dto/AdminAiAssetDetailResponse.java`
  - 상세 응답에 `activeGenerationJob` 필드를 추가했다.
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryRepository.java`
  - 같은 대상의 최신 `QUEUED/RUNNING` generation job을 조회하는 `findActiveGenerationJob()`을 추가했다.
- `qtai-server/service-ai/src/main/java/com/qtai/domain/ai/internal/AdminAiAssetQueryService.java`
  - active job을 상세 응답에 매핑했다.
- `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**`
  - 중복 모듈의 동일 DTO/query/service 변경을 반영했다.
- `admin-web/src/api/aiAssets.ts`
  - `AiAssetDetail.activeGenerationJob` 타입을 추가했다.
- `admin-web/src/pages/AiAssetsPage.tsx`
  - `activeGenerationJob` 또는 기존 `generationJob`이 `QUEUED/RUNNING`이면 재생성 버튼 대신 진행 중 태그를 표시하도록 했다.
  - 기존 로컬 job 상태 보정은 유지했다.
- `qtai-server/apis/api-v1/openapi.yaml`
  - `AdminAiAssetDetailResponse.activeGenerationJob` 스키마를 추가했다.

## 제외한 내용

- 재생성 취소/강제 실패/재시도 기능은 추가하지 않았다.
- Ant Design `destroyOnClose` deprecated 경고는 별도 정리 대상으로 남겼다.

## 검증 결과

```powershell
npm.cmd --prefix admin-web run typecheck
```

결과: 성공

```powershell
.\qtai-server\gradlew.bat -p qtai-server :service-ai:compileJava :admin-server:compileJava
```

결과: 성공

브라우저 확인:

- URL: `http://localhost:5173/ai-assets`
- `AI 산출물 검증` 화면 렌더링 정상
- 콘솔에는 기존 Ant Design `destroyOnClose` deprecated 경고만 확인됨

실행하지 못한 검증:

```powershell
npx.cmd @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset qtai-server/.spectral.yaml
```

결과: 실패. 저장소에 `qtai-server/.spectral.yaml` 또는 루트 `.spectral.yaml` 규칙 파일이 없어 Spectral을 실행할 수 없었다.

## 후속 검토

- active job이 있을 때 상세 본문에도 별도 행으로 보여줄지 검토할 수 있다.
- `destroyOnClose` deprecated 경고는 Modal 전반 정리 작업으로 분리하는 것이 좋다.
