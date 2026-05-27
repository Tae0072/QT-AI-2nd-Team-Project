# Workflow — 2026-05-27 ai-validation-checklist-review-fixes

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-validation-checklist-versions-api` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14 |
| 트리거 | Claude 리뷰에서 `ai_validation_checklist_versions` 관리자 API PR에 BLOCK 1건과 WARN 보강 항목이 제기됨 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**`, `qtai-server/apis/api-v1/openapi.yaml`, `doc/프로젝트 문서/04_API_명세서.md`, `doc/workspaces/DevC_강상민/**` |

## 기준 문서

- `doc/workspaces/DevC_강상민/workflows/2026-05-27_ai-validation-checklist-versions-admin-api.md`
- `doc/workspaces/DevC_강상민/reports/2026-05-27_ai-validation-checklist-versions-admin-api_report.md`
- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `CODE_CONVENTION.md`
- Claude 리뷰 코멘트: JPQL null enum 필터, 활성화 동시성, CONTENT_CREATOR 차단 테스트, 생성 status 검증 책임, `ACTIVE_CHECKLIST_EXISTS` 문서 정리

## 작업 목표

AI 검증 체크리스트 버전 관리 API 리뷰에서 지적된 BLOCK과 저위험 WARN을 반영한다. 핵심은 목록 조회 JPQL의 null enum 파라미터 조건을 제거해 MySQL 방언 리스크를 없애고, 활성화 시 같은 checklistType의 ACTIVE/DRAFT row를 잠가 동시 활성화 race condition을 줄이는 것이다.

추가로 `CONTENT_CREATOR` 차단 테스트를 controller/service 양쪽에 보강하고, 생성 status 검증 책임을 controller 기본값 보정과 service 비즈니스 검증으로 나눈다. 사용하지 않는 `ACTIVE_CHECKLIST_EXISTS` 문서 표기도 제거해 Java ErrorCode/OpenAPI/API 명세 간 혼선을 줄인다.

## 범위

- `AiValidationChecklistVersionRepository.findAllByFilters()`의 `(:param is null or ...)` JPQL을 제거한다.
- null 필터 조합은 service가 분기해 repository의 명시 메서드를 호출하도록 바꾼다.
- `checklistType`, `status`, `checklistType+status`, 필터 없음 4가지 목록 조회 경로를 지원한다.
- 활성화 시 같은 `checklistType` row를 `PESSIMISTIC_WRITE`로 조회하는 repository 메서드를 추가한다.
- `activateAdminAiValidationChecklist()`는 잠금 조회 결과 안에서 target row와 기존 ACTIVE row를 찾아 상태 전이를 수행한다.
- target id가 없으면 `CHECKLIST_NOT_FOUND`, target이 DRAFT가 아니면 `INVALID_STATUS_TRANSITION`을 유지한다.
- `AdminAiValidationChecklistController.normalizeCreateStatus()`는 null/blank만 `DRAFT`로 보정하고, `ACTIVE/RETIRED` 차단은 service의 `requireDraftCreateStatus()`가 담당한다.
- Controller 테스트에 `ADMIN_ROLE_CONTENT_CREATOR` 차단 케이스를 추가한다.
- Service 테스트에 `CONTENT_CREATOR` 차단 케이스를 추가한다.
- Repository 테스트에 필터 없음, type only, status only, type+status 목록 조회와 잠금 조회 결과를 검증한다.
- `04_API_명세서.md`에서 `ACTIVE_CHECKLIST_EXISTS`를 checklist API 현재 에러 표기에서 제거한다.
- `qtai-server/apis/api-v1/openapi.yaml`에 남은 `ACTIVE_CHECKLIST_EXISTS` 참조가 있으면 제거한다.
- DevC report에 리뷰 대응 내용과 검증 결과를 추가한다.

## 제외 범위

- 기존 `AdminAiAssetController`의 인라인 관리자 인증 로직을 `AdminAiAuthentication`으로 통합하는 리팩터링은 제외한다.
- `AuditService.write()`의 `REQUIRES_NEW`, 비동기 처리, `@TransactionalEventListener` 전환은 제외한다.
- MySQL Testcontainers 통합 테스트 추가는 제외한다. JPQL null enum 리스크는 쿼리 분기로 제거한다.
- DB unique partial index로 ACTIVE 단일성을 강제하는 변경은 제외한다. 현재 MySQL/migration 범위를 넘어서는 정책 변경이다.
- 체크리스트 원문 저장, items_json 컬럼, 별도 item 테이블은 제외한다.
- 기존 PR 범위를 넘는 평가 셋/validation_reference_jobs API 구현은 제외한다.

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionRepository.java` | null enum JPQL 제거, 명시 조회 메서드와 잠금 조회 메서드 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistService.java` | 목록 조회 분기, 활성화 잠금 적용, status 검증 책임 정리 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/web/AdminAiValidationChecklistController.java` | `normalizeCreateStatus()`를 기본값 보정만 수행하도록 변경 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionRepositoryTest.java` | 목록 필터 조합과 잠금 조회 테스트 보강 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | CONTENT_CREATOR 차단, repository 분기, 잠금 기반 활성화 테스트 보강 |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiValidationChecklistControllerTest.java` | CONTENT_CREATOR 차단과 status 전달 테스트 보강 |
| Modify | `doc/프로젝트 문서/04_API_명세서.md` | 미사용 `ACTIVE_CHECKLIST_EXISTS` 표기 제거 |
| Modify | `qtai-server/apis/api-v1/openapi.yaml` | 미사용 에러 참조가 있으면 제거 |
| Modify | `doc/workspaces/DevC_강상민/reports/2026-05-27_ai-validation-checklist-versions-admin-api_report.md` | 리뷰 대응 내용과 재검증 결과 기록 |

## 구현 순서

1. `AiValidationChecklistVersionRepositoryTest`에 필터 없음 조회가 `createdAt desc, id desc` 정렬로 전체 row를 반환하는 테스트를 추가한다.
2. 같은 테스트 파일에 type only, status only, type+status 조회가 각각 기대 row만 반환하는 테스트를 추가한다.
3. 같은 테스트 파일에 `findAllByChecklistTypeForUpdate()`가 같은 `checklistType` row만 반환하는 테스트를 추가한다.
4. `AiValidationChecklistVersionRepository`에서 `@Query (:param is null or ...)` 기반 `findAllByFilters()`를 제거한다.
5. `AiValidationChecklistVersionRepository`에 `findAll(Pageable)`, `findByChecklistType(...)`, `findByStatus(...)`, `findByChecklistTypeAndStatus(...)` 페이징 메서드를 사용하거나 선언한다.
6. `AiValidationChecklistVersionRepository`에 `@Lock(PESSIMISTIC_WRITE)`와 `findAllByChecklistTypeOrderByIdAsc()` 계열 메서드를 추가한다.
7. `AdminAiValidationChecklistService.listAdminAiValidationChecklists()`가 null 여부에 따라 4가지 repository 메서드 중 하나를 호출하도록 분기한다.
8. `AdminAiValidationChecklistServiceTest`에서 목록 조회 필터 조합이 repository의 올바른 메서드로 전달되는지 검증한다.
9. `AdminAiValidationChecklistService.activateAdminAiValidationChecklist()`에서 target을 먼저 `findById()`로 조회하는 흐름을 잠금 조회 결과 기반으로 변경한다.
10. 활성화 service 테스트를 `findAllByChecklistTypeForUpdate()` 기반 mock으로 갱신하고, target DRAFT와 기존 ACTIVE가 같은 잠금 조회 결과에서 처리되는지 검증한다.
11. target id가 잠금 조회 결과에 없을 때 `CHECKLIST_NOT_FOUND`가 발생하는 테스트를 유지하거나 보강한다.
12. controller의 `normalizeCreateStatus()`에서 `ACTIVE/RETIRED` 차단을 제거하고 null/blank 기본값만 처리한다.
13. controller 테스트에서 `status=ACTIVE` 요청이 UseCase까지 전달되고, UseCase가 `INVALID_INPUT`을 던질 때 400으로 매핑되는지 검증한다.
14. controller 권한 테스트에 `ADMIN_ROLE_CONTENT_CREATOR` 요청이 403으로 차단되는지 추가한다.
15. service 권한 테스트에 `CONTENT_CREATOR`가 403으로 차단되는지 추가한다.
16. `04_API_명세서.md`의 공통 에러 표와 7.2 설명에서 `ACTIVE_CHECKLIST_EXISTS`를 제거하고 자동 retire 정책만 남긴다.
17. OpenAPI에 `ACTIVE_CHECKLIST_EXISTS` 참조가 있으면 제거한다.
18. report에 리뷰 대응 항목, 제외한 후속 작업, 검증 명령 결과를 기록한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionRepositoryTest.java` | 필터 없음 목록 조회가 전체 row를 정렬 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionRepositoryTest.java` | `checklistType` only, `status` only, `checklistType+status` 조회가 각각 동작한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiValidationChecklistVersionRepositoryTest.java` | 활성화 잠금 조회 메서드가 같은 `checklistType` row만 반환한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | `CONTENT_CREATOR`가 목록 조회 권한에서 차단된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | 목록 조회 필터 조합별 repository 분기가 올바르다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AdminAiValidationChecklistServiceTest.java` | 활성화가 잠금 조회 결과를 사용해 기존 ACTIVE retire와 target activate를 수행한다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiValidationChecklistControllerTest.java` | `ADMIN_ROLE_CONTENT_CREATOR`가 403으로 차단된다 |
| `qtai-server/src/test/java/com/qtai/domain/ai/web/AdminAiValidationChecklistControllerTest.java` | `status=ACTIVE`는 controller에서 직접 차단하지 않고 UseCase error mapping으로 400 처리된다 |

## 수용 기준

- [ ] `findAllByFilters()`의 null enum JPQL이 제거된다.
- [ ] 목록 조회 4가지 필터 조합이 모두 테스트된다.
- [ ] 활성화는 같은 `checklistType` row를 `PESSIMISTIC_WRITE`로 잠근 뒤 처리한다.
- [ ] `CONTENT_CREATOR` 차단 테스트가 controller/service에 모두 추가된다.
- [ ] controller는 생성 status 기본값 보정만 수행하고 business 차단은 service가 담당한다.
- [ ] `ACTIVE_CHECKLIST_EXISTS` 미사용 표기가 기준 API 문서에서 제거된다.
- [ ] 금지 import, 금지 기술, 금지 데이터가 추가되지 않는다.
- [ ] 기존 체크리스트 API 성공/실패 테스트가 계속 통과한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- Repository 메서드 변경, Service 호출 분기, 테스트 mock 갱신이 강하게 연결되어 있어 순차 반영이 안전하다.
- 활성화 잠금 흐름은 상태 전이와 감사 로그 호출 순서까지 함께 맞춰야 하므로 한 작업자가 맥락을 유지하는 편이 재작업을 줄인다.
- 문서 변경은 코드 변경 결과와 정확히 맞아야 하므로 최종 단계에서 함께 반영한다.

### 위임 가능 작업

| Worker | 독립 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 repository test, service test, controller test, 구현, 문서 정리, 최종 검증을 순서대로 직접 수행한다.

## 검증 계획

- `Push-Location qtai-server; .\gradlew.bat test --tests "*AiValidationChecklistVersionRepositoryTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AdminAiValidationChecklistServiceTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*AdminAiValidationChecklistControllerTest"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat test --tests "*Ai*"; Pop-Location`
- `Push-Location qtai-server; .\gradlew.bat build; Pop-Location`
- `npx.cmd @apidevtools/swagger-cli validate qtai-server/apis/api-v1/openapi.yaml`
- `rg -n "^import .*domain\\.[a-z]+\\.(internal|web|repository)" qtai-server/src/main/java/com/qtai/domain/ai`
- `rg -n "ACTIVE_CHECKLIST_EXISTS" qtai-server/src/main/java qtai-server/apis/api-v1/openapi.yaml "doc/프로젝트 문서/04_API_명세서.md"`
- `rg -n "javax\\.|SseEmitter|/ai/sessions|ChromaDB|Elasticsearch|Kafka|개역개정|ESV|NIV|성서유니온|두란노" qtai-server/src/main/java qtai-server/src/test/java qtai-server/apis/api-v1/openapi.yaml`

## 후속 작업으로 남길 항목

- 기존 `AdminAiAssetController` 관리자 인증 로직을 `AdminAiAuthentication`으로 통합
- 감사 로그 저장을 비즈니스 트랜잭션에서 분리할지 검토
- MySQL Testcontainers 기반 동시 활성화 통합 테스트 또는 ACTIVE 단일성 DB 제약 검토
