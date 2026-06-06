# Report - 2026-06-02 ai-admin-monitoring-summary-api

## Summary

- `GET /api/v1/admin/ai/monitoring` 관리자 운영 모니터링 집계 API를 추가했다.
- 권한은 `ADMIN + OPERATOR/REVIEWER/SUPER_ADMIN`로 고정했다.
- 신규 DB schema/migration은 추가하지 않았다.
- Q&A 집계는 `ai_qa_requests` 미구현 상태를 반영해 0값과 빈 목록으로 반환한다.
- OpenAPI와 `04_API_명세서.md`에 신규 endpoint/schema와 집계 기준을 반영했다.

## 구현 내용

- `domain.ai.api`
  - `GetAdminAiMonitoringUseCase`
  - `GetAdminAiMonitoringQuery`
  - `AdminAiMonitoringResponse`
- `domain.ai.web`
  - `AdminAiMonitoringController`
  - `AdminAiAuthentication.requireMonitoring(...)` 기반 권한 검증
- `domain.ai.internal`
  - `AdminAiMonitoringQueryService`
  - `AdminAiMonitoringQueryRepository`
- 집계 기준
  - `queued/running`: 현재 active backlog
  - `succeeded/failed`: 기간 내 `finishedAt`
  - `waitingAssets`: 현재 `VALIDATING` asset
  - validation: 기간 내 `ai_validation_logs.createdAt`
  - batchRuns: 기간 내 `ai_batch_run_logs.createdAt`
  - checklists: active checklist version별 기간 내 passRate

## TDD 기록

- RED:
  - `.\gradlew.bat test --tests "*AdminAiMonitoring*" --tests "*AiUseCaseContractTest"`
  - 신규 UseCase/DTO/controller/service/repository 부재로 컴파일 실패 확인
- GREEN:
  - API DTO, controller, query service, aggregate repository 구현
  - controller/service/repository/contract 테스트 통과 확인

## 검증 결과

```powershell
cd qtai-server
.\gradlew.bat test --tests "*AdminAiMonitoring*" --tests "*AiUseCaseContractTest" --tests "*AdminAiBatchRunLog*"
.\gradlew.bat build
cd ..
git diff --check
rg -n "^import .*domain\.[a-z]+\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai
python -c "import yaml, pathlib; yaml.safe_load(pathlib.Path('qtai-server/apis/api-v1/openapi.yaml').read_text(encoding='utf-8')); print('openapi yaml ok')"
```

- 관련 테스트: PASS
- 전체 build: PASS
- `git diff --check`: PASS
- 금지 import 검색: 매치 없음
- OpenAPI YAML parse: PASS
- `.spectral.yaml`: 저장소에 없어 Spectral lint는 실행하지 않음

## 참고

- 운영 집계 API는 raw prompt, provider raw response, asset payload/content, secret/token/password 계열 값을 반환하지 않는다.
- batch 상세 실행 로그 목록은 기존 `/api/v1/admin/ai/batch-run-logs`를 계속 사용한다.
- 관리자 조회 감사 로그 write는 이번 PR 범위에서 제외했다.
