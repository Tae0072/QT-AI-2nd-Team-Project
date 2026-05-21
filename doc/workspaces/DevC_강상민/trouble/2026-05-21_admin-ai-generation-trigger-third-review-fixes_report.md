# Report — 2026-05-21 admin-ai-generation-trigger-third-review-fixes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-admin-generation-trigger` |
| PR 대상 | `dev` |
| 관련 PR | `feat(ai): AI 생성 작업 큐잉과 관리자 재생성 요청 추가 #40` |
| 실행 경로 | 3차 PR 리뷰 REQUEST_CHANGES 대응 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-21_admin-ai-generation-trigger-api.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-21_create-ai-generation-job-usecase-implementation.md` |
| 관련 F-ID | F-02, F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/trouble/2026-05-21_admin-ai-generation-trigger-third-review-fixes_report.md` |

## 증상

3차 PR 리뷰에서 관리자 AI 재생성 트리거 API의 UseCase 입력 검증, DB 멱등성 migration sequencing, negative test, DB integrity 예외 매핑 범위에 대해 머지 전 보강 요청을 받았다.

핵심 차단 항목은 다음 두 가지였다.

1. `RegenerateAiAssetCommand`의 Bean Validation 규칙과 `AiService`의 수동 검증이 일치하지 않아, UseCase를 직접 호출하는 내부 경로에서 잘못된 입력이 `FORBIDDEN` 또는 예기치 않은 예외로 새어 나갈 수 있다.
2. `active_unique_key` unique index는 Entity에 선언됐지만 운영 MySQL migration이 없어, 운영 환경에서 F-14 멱등성 방어선이 실제로 적용되지 않을 수 있다.

## 원인

`RegenerateAiAssetCommand` record에는 `memberRole`, `adminRole`에 `@NotBlank`, `promptVersionId`에 `@Positive`가 선언돼 있다. 하지만 UseCase interface를 직접 호출하면 record의 Bean Validation은 자동 실행되지 않는다. 실제 방어선은 `AiService.requireValidCommand(...)`인데, 기존 구현은 `memberRole`, `adminRole` blank 검증을 누락했다.

`DataIntegrityViolationException`은 DB unique race를 `INVALID_STATUS_TRANSITION`으로 바꾸기 위해 잡고 있었지만, 모든 integrity 오류를 같은 비즈니스 예외로 변환했다. 이 경우 not-null, FK, 다른 unique constraint 오류까지 진행 중 job 중복처럼 보일 수 있다.

운영 migration은 현재 저장소에 Flyway/Liquibase 또는 SQL migration 실행 구조가 없어, 이번 PR에서 임의로 Flyway를 붙이면 기존 스키마 baseline과 부트 순서 문제가 생길 수 있다. 따라서 이 항목은 PR 본문에 머지 전 sequencing을 명확히 남기는 방식으로 처리한다.

## 해결

즉시 코드로 보강 가능한 항목은 다음과 같이 수정했다.

1. `AiService.requireValidCommand(RegenerateAiAssetCommand)`에서 `memberRole`, `adminRole` blank 검증을 추가했다.
2. `promptVersionId` 양수 검증은 유지해 command DTO의 `@Positive`와 서비스 검증을 맞췄다.
3. `DataIntegrityViolationException`은 메시지 또는 most specific cause에 `uk_ai_generation_jobs_active_target_prompt`가 포함된 경우에만 `INVALID_STATUS_TRANSITION`으로 변환한다.
4. 다른 integrity 오류는 원래 `DataIntegrityViolationException`으로 그대로 전파해 원인 오분류를 막는다.
5. 서비스 negative test, principal 파싱 실패 테스트, `SecurityContextHolder` fallback 테스트를 추가했다.

## 변경 요약

| 파일 | 변경 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiService.java` | `RegenerateAiAssetCommand`의 `memberRole/adminRole` 수동 검증 추가, active job unique constraint 오류만 비즈니스 예외로 매핑 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiServiceTest.java` | regenerate command invalid field negative test, active unique constraint race 매핑 테스트, unrelated integrity 오류 비매핑 테스트 추가 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiAssetControllerTest.java` | principal id 파싱 실패 401 테스트, `SecurityContextHolder` fallback 테스트 추가 |

## 리뷰 지적별 대응

| 리뷰 지적 | 대응 |
| --- | --- |
| `RegenerateAiAssetCommand` 검증 규칙과 서비스 검증 불일치 | 서비스 수동 검증에 `memberRole`, `adminRole`, `promptVersionId` 검증을 맞춤 |
| 잘못된 UseCase 호출이 `FORBIDDEN`으로 새어 나갈 수 있음 | blank role은 권한 판단 전 `INVALID_INPUT`으로 차단 |
| `active_unique_key` 운영 migration 부재 | 현재 repo에 migration 도구가 없으므로 PR 본문에 선행 migration sequencing을 BLOCK 조건으로 명시 |
| command negative test 누락 | blank `memberRole`, blank `adminRole`, non-positive `promptVersionId` 테스트 추가 |
| `parsePrincipalId` 실패 케이스 누락 | non-numeric principal이면 `UNAUTHORIZED`이고 UseCase 미호출 테스트 추가 |
| `SecurityContextHolder` fallback 테스트 누락 | method argument authentication이 없을 때 SecurityContext 인증을 사용하는 테스트 추가 |
| `DataIntegrityViolationException` 광범위 매핑 | 명명된 active unique constraint 충돌만 비즈니스 예외로 변환하고 나머지는 원 예외 유지 |

## PR 본문 보강 문구

아래 문구는 PR 본문에 머지 전 조건으로 명시해야 한다.

```markdown
## 머지 전 sequencing
- `active_unique_key` 운영 migration: 현재 저장소에는 Flyway/Liquibase 등 DB migration 실행 구조가 없어 `@Table(uniqueConstraints=...)`만으로 운영 MySQL에 `uk_ai_generation_jobs_active_target_prompt`가 생성된다고 보장할 수 없다. 본 PR을 운영 반영 또는 최종 merge하기 전 `ai_generation_jobs.active_unique_key` 컬럼과 `job_type + target_type + target_id + prompt_version + active_unique_key` unique index를 별도 migration PR로 선반영하거나, migration 인프라 PR이 먼저 merged된 뒤 본 PR에 migration 파일을 추가한다. 이 sequencing이 완료되지 않으면 F-14 멱등성 보장은 운영에서 성립하지 않는다.
```

## 검증 예정

| 명령 | 목적 |
| --- | --- |
| `gradle -p qtai-server test --tests "*AdminAiAssetControllerTest" --tests "*AiServiceTest" --tests "*AiGenerationJobTest"` | 이번 리뷰 대응 관련 단위 테스트 검증 |
| `gradle -p qtai-server clean build --no-build-cache` | 전체 서버 빌드와 테스트 회귀 검증 |
| `git diff --check` | 공백 오류 검증 |

## 배운 점

DTO의 Bean Validation 어노테이션은 Controller request body 검증과 달리 UseCase 직접 호출에는 자동 적용되지 않는다. 내부 UseCase 경계에서 command를 받는 서비스는 DTO 어노테이션과 동일한 수동 검증 또는 명시적인 validation adapter를 가져야 한다.

DB race condition을 잡기 위해 integrity 예외를 변환할 때도 변환 범위를 constraint 이름으로 좁혀야 한다. 그렇지 않으면 unrelated DB 오류가 사용자에게 중복 요청처럼 보이고, 실제 장애 원인을 숨길 수 있다.
