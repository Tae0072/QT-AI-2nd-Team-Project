# Report - 2026-06-09 ai-service-usecase-persistence-skeleton

## 작업 요약

`ai-service` inbound API가 opt-in 조건에서 실제 ai-service 소유 DB repository 기반 UseCase 구현체를 주입받도록 persistence skeleton을 연결했다. 기본값에서는 inbound와 persistence가 모두 비활성화되어 기존 실행/CI/운영 기본 동작은 바뀌지 않는다.

이번 작업은 MSA 물리 분리 준비를 위한 skeleton 연결이며, 실제 provider live 호출, DeepSeek worker 이관, gateway route 전환, 운영 데이터 이관, monolith AI 코드 제거는 수행하지 않았다.

## 변경 내용

- `AiServiceUseCaseConfiguration`을 추가해 `qtai.ai.inbound.enabled=true`와 `qtai.ai.persistence.enabled=true`가 모두 켜질 때만 internal UseCase service/query repository를 등록하도록 했다.
- system UseCase 구현체를 ai-service 내부에 추가했다.
  - generation job 생성
  - generated asset 등록
  - validation log 등록
  - validation reference job create/get/expire
- admin UseCase 구현체를 ai-service 내부에 추가했다.
  - asset list/detail/review/regenerate
  - validation checklist list/create/activate/retire
  - monitoring 조회
  - batch run log 조회
- monolith 직접 의존은 사용하지 않고 `AuditLogClient`, `StudyPublishClient`를 통해 outbound 경계를 유지했다.
- admin read model 조회에 필요한 query repository를 ai-service internal 영역에 추가했다.
- `qtai-server/apis/ai-service/openapi.yaml`의 inbound skeleton 설명을 persistence opt-in 구현 상태로 갱신했다.
- validation log result enum을 실제 internal enum과 맞게 `PASSED`, `REJECTED`, `NEEDS_REVIEW`로 정리했다.
- 기존 controller skeleton 테스트의 validation fixture를 실제 계약값인 `PASSED`, `AUTO`로 정리했다.

## 테스트 보강

- `AiServiceUseCasePersistenceContextTest`
  - inbound+persistence opt-in 조건에서 mock usecase 없이 실제 UseCase bean과 controller가 등록되는지 검증했다.
- `AiServiceSystemUseCasePersistenceTest`
  - prompt/checklist seed 후 generation job, asset, validation log, validation reference job 저장/조회/만료를 검증했다.
- `AiServiceAdminUseCasePersistenceTest`
  - asset list/detail, checklist create/activate/retire, monitoring, batch log 조회를 검증했다.
- `AiServiceAssetReviewPersistenceTest`
  - approve/hide/regenerate/reject 상태 전이와 audit/study outbound client mock 호출을 검증했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat :ai-service:compileJava` | 통과 |
| `.\gradlew.bat :ai-service:compileTestJava` | 통과 |
| `.\gradlew.bat :ai-service:test` | 통과 |
| `git diff --check` | 통과 |
| api/web 금지 import 검사 | 통과 |
| ai-service 금지 데이터/민감 예시 검사 | 통과 |

## 제외 확인

- 기존 monolith AI 코드는 삭제하지 않았다.
- 실제 provider endpoint 호출은 추가하지 않았다.
- DeepSeek worker, scheduler, gateway route, service-token/JWKS는 변경하지 않았다.
- 운영 DB 연결이나 데이터 이관은 수행하지 않았다.
- `api`/`web` 경계에 다른 도메인의 repository/entity/internal 타입을 노출하지 않았다.

## 다음 작업

- provider endpoint가 열리면 opt-in smoke test를 실제 base-url/service token으로 실행한다.
- ai-service worker/DeepSeek flow 이관 여부를 별도 PR로 결정한다.
- gateway route 전환과 운영 DB 이관은 live smoke 이후 진행한다.
