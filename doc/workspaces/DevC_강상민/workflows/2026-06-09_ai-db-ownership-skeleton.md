# Workflow - 2026-06-09 ai-db-ownership-skeleton

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 작업명 | `ai-db-ownership-skeleton` |
| 기준 브랜치 | `dev` |
| 작업 브랜치 | `feature/ai-db-ownership-skeleton` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 트리거 | `ai-service` 물리 분리 이후 AI 소유 DB 모델을 새 서비스가 자체 보유할 skeleton이 필요함 |
| 기준 문서 | `2026-06-08_ai-service-boundary-contract.md`, `2026-06-08_ai-service-extraction-skeleton.md`, `2026-06-09_ai-service-inbound-api-skeleton.md` |
| 대상 경로 | `qtai-server/ai-service/**`, `doc/workspaces/DevC_강상민/**` |

## 작업 목표

`ai-service` 모듈이 AI 소유 DB 모델을 자체 보유할 수 있도록 JPA entity/repository skeleton을 추가한다. 이번 단계는 소유권 경계와 컴파일 가능한 persistence 기반을 만드는 것이며, 운영 Flyway migration이나 실제 데이터 이관은 수행하지 않는다.

기본 실행에서는 DB가 강제 연결되지 않도록 persistence는 opt-in으로 둔다. `qtai.ai.persistence.enabled=true`와 H2 설정을 준 테스트에서만 repository와 entity 매핑을 검증한다.

## 범위

- `ai-service`에 AI 소유 JPA entity, repository, support enum, guard를 복제한다.
- 포함 테이블은 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`, `ai_prompt_versions`, `ai_validation_checklist_versions`, `validation_reference_jobs`, `ai_batch_run_logs`다.
- `AiServicePersistenceConfiguration`으로 JPA repository/entity scan을 opt-in 활성화한다.
- `application.yml`에 `qtai.ai.persistence.*` placeholder를 추가한다.
- H2 기반 테스트로 repository bean 등록, JPA metamodel, 대표 save/query, batch log auditing을 검증한다.
- report에 AI 소유 DB와 비소유 DB를 명확히 기록한다.

## 제외 범위

- 기존 monolith AI entity/repository 삭제 또는 이동
- 운영 Flyway migration 생성
- prompt seed 운영 이관
- 실제 MySQL 연결 검증
- 데이터 복제, 백필, 마이그레이션
- gateway route, JWKS, service-token 발급
- provider endpoint 연결
- AI business logic, scheduler, worker, LLM handler 이관
- 다른 도메인 DB 직접 조회 추가

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/ai-service/build.gradle.kts` | JPA, MySQL runtime, H2 test dependency 추가 |
| Modify | `qtai-server/ai-service/src/main/resources/application.yml` | persistence opt-in property와 DB placeholder 추가 |
| Modify | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServiceApplication.java` | persistence configuration import |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServicePersistenceConfiguration.java` | opt-in JPA datasource, entity manager, transaction manager, repository scan |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/ai/AiServicePersistenceProperties.java` | persistence 설정과 fail-fast 검증 |
| Create | `qtai-server/ai-service/src/main/java/com/qtai/domain/ai/internal/**` | AI 소유 entity/repository/support enum/guard skeleton |
| Create | `qtai-server/ai-service/src/test/java/com/qtai/domain/ai/internal/**` | persistence disabled/enabled, repository, metamodel 테스트 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-09_ai-db-ownership-skeleton_report.md` | 작업 결과와 검증 결과 기록 |

## 구현 순서

1. `dev` 최신화 후 `feature/ai-db-ownership-skeleton` 브랜치에서 작업한다.
2. workflow 문서를 저장하고 변경 범위와 제외 범위를 고정한다.
3. `ai-service` build/config에 persistence opt-in 기반을 추가한다.
4. monolith AI internal에서 합의된 entity, repository, enum, guard만 `ai-service`로 복제한다.
5. `AiServicePersistenceConfiguration`과 `AiServicePersistenceProperties`를 추가한다.
6. 기본 context에서는 repository가 등록되지 않는 테스트를 추가한다.
7. persistence opt-in H2 context에서 7개 repository와 7개 entity metamodel을 검증한다.
8. 대표 repository save/query와 `ai_batch_run_logs.createdAt` auditing을 검증한다.
9. 검증 명령을 실행하고 report에 결과를 기록한다.
10. 지정 파일만 stage 후 `feat(ai): ai-service DB 소유권 skeleton 추가`로 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiServicePersistenceDisabledContextTest` | 기본 property에서 persistence repository bean 미등록 |
| `AiServicePersistenceEnabledContextTest` | H2 opt-in에서 7개 repository bean과 7개 entity metamodel 등록 |
| `AiServicePersistenceRepositoryTest` | 대표 save/query, validation reference 조회, checklist 조회, batch log auditing |
| `AiServicePersistenceFailFastTest` | `enabled=true`인데 DB URL 누락 시 fail-fast |

## 수용 기준

- [ ] `:ai-service:compileJava`가 통과한다.
- [ ] `:ai-service:test`가 통과한다.
- [ ] 기본 `qtai.ai.persistence.enabled=false`에서 DB 연결이 강제되지 않는다.
- [ ] `qtai.ai.persistence.enabled=true`와 H2 설정에서 7개 repository가 등록된다.
- [ ] JPA metamodel에 7개 AI 소유 entity가 포함된다.
- [ ] `ai_batch_run_logs` auditing `createdAt`이 저장 시 채워진다.
- [ ] `ai-service` persistence 코드가 다른 도메인 `internal`, `Repository`, `Entity`를 직접 import하지 않는다.
- [ ] 운영 Flyway migration과 실제 데이터 이관 코드를 추가하지 않는다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- JPA 설정, entity/repository 복제, 테스트가 같은 persistence 경계를 공유한다.
- 병렬 작업으로 나누면 config와 테스트의 bean 조건 판단이 어긋날 위험이 있다.
- 이번 PR은 skeleton 경계를 작게 고정하는 작업이라 직접 실행이 더 안전하다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow 작성, persistence skeleton 구현, 테스트 추가, report 작성, 검증, 커밋을 직접 수행한다.

## 검증 계획

```powershell
cd qtai-server
.\gradlew.bat :ai-service:compileJava
.\gradlew.bat :ai-service:test
cd ..
git diff --check
$placeholderPattern = "TB" + "D|TO" + "DO|추" + "후 정리|나" + "중에 정리|미" + "정"
rg -n $placeholderPattern "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-db-ownership-skeleton.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-db-ownership-skeleton_report.md"
$restrictedPattern = "개역" + "개정|ES" + "V|NI" + "V|성서" + "유니온|두" + "란노|plain " + "secret|private " + "key"
rg -n $restrictedPattern "qtai-server\ai-service" "doc\workspaces\DevC_강상민\workflows\2026-06-09_ai-db-ownership-skeleton.md" "doc\workspaces\DevC_강상민\reports\2026-06-09_ai-db-ownership-skeleton_report.md"
rg --pcre2 -n "com\.qtai\.domain\.(?!ai)[a-z]+\.internal|com\.qtai\.domain\.(?!ai)[a-z]+.*Repository|com\.qtai\.domain\.(?!ai)[a-z]+.*Entity" "qtai-server\ai-service\src\main\java\com\qtai\domain\ai\internal"
```

## 후속 작업으로 남긴 항목

- `ai-service` 운영 Flyway migration 작성
- prompt version seed 이관 또는 bootstrap 정책 확정
- monolith와 `ai-service` DB 데이터 이관 절차 작성
- 기존 monolith AI persistence 제거 시점 결정
- 실제 MySQL 연결 smoke test
