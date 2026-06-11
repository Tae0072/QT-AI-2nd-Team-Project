# Workflow - 2026-06-09 ai-service-usecase-persistence-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-service-usecase-persistence-skeleton` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `feature/ai-service-usecase-persistence-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | `ai-service`에 inbound API, DB entity/repository, migration skeleton이 들어왔으므로 실제 UseCase bean을 ai-service DB에 연결해야 한다. |
| 기준 문서 | `2026-06-09_ai-service-inbound-api-skeleton.md`, `2026-06-09_ai-db-ownership-skeleton.md`, `2026-06-09_ai-service-db-migration-skeleton.md` |
| 대상 경로 | `qtai-server/ai-service/**`, `qtai-server/apis/ai-service/openapi.yaml`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`ai-service`의 opt-in inbound Controller가 mock/fake UseCase 없이 ai-service 소유 Repository 기반 UseCase 구현체로 동작할 수 있게 한다. 기본 실행에서는 기존처럼 inbound와 persistence가 모두 꺼져 있어 DB 연결과 endpoint 노출이 강제되지 않는다.

이번 단계는 MSA 물리 분리 skeleton의 persistence 연결까지가 목적이다. 실제 DeepSeek 실행, provider live endpoint 호출, gateway route 전환, 운영 데이터 이관, monolith AI 코드 제거는 하지 않는다.

## 범위

- `qtai.ai.inbound.enabled=true`와 `qtai.ai.persistence.enabled=true`가 모두 켜질 때만 UseCase 구현체를 등록한다.
- system inbound UseCase를 ai-service repository 기반으로 구현한다.
- admin asset, checklist, monitoring, batch-run-log UseCase를 ai-service repository/query 기반으로 구현한다.
- monolith에서 직접 사용하던 `WriteAuditLogUseCase`, study publish/hide usecase는 ai-service outbound client인 `AuditLogClient`, `StudyPublishClient`로 대체한다.
- admin list/detail/monitoring/batch 조회에 필요한 query repository를 ai-service internal 영역에 추가한다.
- OpenAPI extension의 inbound skeleton 설명을 persistence opt-in 구현 상태로 갱신한다.

## 제외 범위

- 기존 `qtai-server` monolith AI 코드 삭제 또는 이동
- 실제 DeepSeek/LLM worker 이관
- 실제 provider endpoint live 호출
- gateway route를 ai-service로 전환
- 운영 DB 연결, 데이터 이관, seed/backfill
- service-token 발급, JWKS, Docker/K8s
- `com.qtai.domain.audit.*`, `com.qtai.domain.study.api.*`, `com.qtai.domain.admin.*` 직접 import

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceUseCaseConfiguration.java` | inbound+persistence opt-in 조건에서 UseCase service/query repository 등록 |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceApplication.java` | UseCase configuration import |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/*Service.java` | system/admin UseCase 구현체 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/*QueryRepository.java` | admin read model 조회 |
| Modify | `qtai-server/apis/ai-service/openapi.yaml` | inbound skeleton 구현 상태 extension 갱신 |
| Test | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/*PersistenceTest.java` | H2 기반 UseCase persistence 검증 |
| Modify | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/web/AiServiceSystemInboundControllerTest.java` | validation fixture 값을 실제 계약값으로 정리 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-service-usecase-persistence-skeleton_report.md` | 작업 결과와 검증 기록 |

## 구현 순서

1. 작업 브랜치를 `dev` 최신 상태에서 생성한다.
2. workflow 문서를 저장하고 변경 범위와 제외 범위를 고정한다.
3. `AiServiceUseCaseConfiguration`을 추가해 UseCase 구현체와 `Clock` bean을 opt-in으로 등록한다.
4. system UseCase 구현체를 추가한다: generation job 생성, generated asset 등록, validation log 등록, validation reference job create/get/expire.
5. admin UseCase 구현체를 추가한다: asset review/regenerate, checklist list/create/activate/retire, monitoring, batch-run-log list.
6. monolith 직접 의존은 `AuditLogClient`, `StudyPublishClient`로 치환한다.
7. admin query repository를 ai-service internal 영역에 추가한다.
8. OpenAPI extension을 persistence opt-in 구현 상태로 갱신한다.
9. H2 기반 UseCase persistence 테스트를 추가하고 기존 controller fixture를 실제 계약값으로 정리한다.
10. 검증 명령을 실행하고 report에 결과를 기록한다.
11. 지정된 파일만 stage 후 `feat(ai): ai-service usecase persistence skeleton 추가`로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiServiceUseCasePersistenceContextTest` | inbound+persistence enabled에서 실제 UseCase bean과 controller가 mock 없이 등록됨 |
| `AiServiceSystemUseCasePersistenceTest` | prompt seed 후 generation job, asset, validation log, validation reference job 저장/조회/만료 |
| `AiServiceAdminUseCasePersistenceTest` | asset list/detail, checklist create/activate/retire, monitoring, batch log 조회 |
| `AiServiceAssetReviewPersistenceTest` | approve/reject/hide/regenerate 상태 전이와 outbound client mock 호출 |
| `AiServiceSystemInboundControllerTest` | validation result/reviewer fixture를 `PASSED`, `AUTO`, `ADVISOR` 계약값으로 정리 |

## 수용 기준

- [ ] 기본 실행에서 inbound/persistence가 비활성화되어 기존 안전 기본값이 유지된다.
- [ ] opt-in 조건에서 ai-service Controller가 실제 UseCase 구현체를 주입받는다.
- [ ] UseCase 구현체가 ai-service 소유 Repository만 사용한다.
- [ ] audit/study side effect는 Java usecase 직접 import가 아니라 outbound client를 사용한다.
- [ ] H2 기반 테스트에서 system/admin 대표 persistence flow가 통과한다.
- [ ] OpenAPI extension이 현재 구현 상태와 모순되지 않는다.
- [ ] 금지 번역본/민감 키워드/금지 import가 추가되지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- UseCase, query repository, outbound client 대체, 통합 테스트가 같은 persistence 흐름에 묶여 있다.
- 병렬 작업으로 나누면 enum/DTO/상태 전이 정합성이 깨질 가능성이 높다.
- 이번 PR은 skeleton 연결 범위를 작게 유지해야 하므로 직접 실행이 안전하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, 구현, 테스트 보강, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test
cd ..
git diff --check
rg -n "com\.qtai\.domain\.audit|com\.qtai\.domain\.study\.api|com\.qtai\.domain\.admin|Repository|@Entity" qtai-server\ai-service\src\main\java\com\qtai\domain\ai\api qtai-server\ai-service\src\main\java\com\qtai\domain\ai\web
$restrictedPattern = "개역" + "개정|ES" + "V|NI" + "V|성서" + "유니온|두란" + "노|plain " + "secret|private " + "key"
rg -n $restrictedPattern qtai-server\ai-service "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-service-usecase-persistence-skeleton.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-service-usecase-persistence-skeleton_report.md"
```

## 후속 작업으로 남길 항목

- 실제 provider endpoint가 열린 뒤 HTTP integration smoke test 실행
- ai-service worker/DeepSeek flow 이관
- gateway route 전환과 service-token/JWKS 적용
- 운영 DB migration 적용과 monolith DB 데이터 이관
- monolith AI persistence/controller 제거 시점 결정
