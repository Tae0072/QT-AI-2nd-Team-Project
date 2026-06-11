# Workflow — 2026-06-11 ai-evaluation-cases

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-evaluation-cases` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14 |
| 트리거 | TODO 2. `[P1] AI 평가 셋/케이스 관리자 관리 구현` |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md`, ERD 문서, `AGENTS.md` |
| 해당 경로 | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/admin-server/src/main/resources/db/migration/**`, `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

관리자가 AI 평가 셋과 평가 케이스를 생성, 조회, 검토할 수 있도록 `admin-server`의 AI 관리자 패턴에 맞춰 도메인, API, DB 마이그레이션, 테스트를 추가한다. 케이스 승인/반려는 감사 로그를 남기고, asset payload 기반 candidate 생성은 원문과 민감 정보를 저장하지 않는 최소 스냅샷 방식으로 연결한다.

## 범위

- `ai_evaluation_sets`, `ai_evaluation_cases` 테이블 추가
- evaluation set 생성, 목록 조회, 상세 조회, 활성화 가능 여부 확인, 활성화, 은퇴
- evaluation case 생성, 목록 조회, 상세 조회, approve, reject
- 관리자 API 추가
  - `GET /api/v1/admin/ai/evaluation-sets`
  - `POST /api/v1/admin/ai/evaluation-sets`
  - `GET /api/v1/admin/ai/evaluation-sets/{setId}`
  - `POST /api/v1/admin/ai/evaluation-sets/{setId}/activate`
  - `POST /api/v1/admin/ai/evaluation-sets/{setId}/retire`
  - `GET /api/v1/admin/ai/evaluation-sets/{setId}/cases`
  - `POST /api/v1/admin/ai/evaluation-sets/{setId}/cases`
  - `GET /api/v1/admin/ai/evaluation-cases/{caseId}`
  - `POST /api/v1/admin/ai/evaluation-cases/{caseId}/approve`
  - `POST /api/v1/admin/ai/evaluation-cases/{caseId}/reject`
  - `POST /api/v1/admin/ai/assets/{assetId}/evaluation-candidates`
- 케이스 approve/reject 감사 로그 기록
  - `EVAL_CASE_APPROVE`
  - `EVAL_CASE_REJECT`
- 서비스와 컨트롤러 테스트 보강

## 제외 범위

- 관리자 웹 UI 구현
- OpenAPI 문서 갱신
- 기존 AI asset 조회/생성 플로우 리팩터링
- `AiEvaluationService`의 Set/Case/Asset 서비스 분리
- `findSetPage` 동적 조회를 Specification 또는 QueryDSL로 전환
- PR 생성과 원격 push

## 주요 설계 결정

| 구분 | 결정 |
| --- | --- |
| 패키지 | 공개 계약은 `domain.ai.api.admin.evaluation`, 구현은 `domain.ai.internal`, HTTP는 `domain.ai.web`에 둔다. |
| 권한 | 평가 셋/케이스 생성과 조회는 `ADMIN + CONTENT_CREATOR/REVIEWER/SUPER_ADMIN`, approve/reject는 `ADMIN + REVIEWER/SUPER_ADMIN`으로 제한한다. |
| 상태 전이 | set은 `DRAFT -> ACTIVE -> RETIRED`, case는 `CANDIDATE -> APPROVED/REJECTED`만 허용한다. |
| 활성화 조건 | set 활성화는 승인된 케이스 10건 이상일 때만 허용한다. 미달 시 `INVALID_STATUS_TRANSITION`으로 처리한다. |
| 중복 방어 | `(eval_type, version)` unique 제약과 서비스의 `DataIntegrityViolationException` 매핑을 함께 둔다. |
| 감사 로그 | approve/reject 시 before/after snapshot을 기록하고, after snapshot에 `reviewReason`을 포함한다. |
| asset candidate | asset 식별자와 대상 메타데이터만 저장하고 prompt 원문, provider raw response, secret, 검증 주석 원문은 저장하지 않는다. |

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/admin-server/src/main/resources/db/migration/V35__create_ai_evaluation_tables.sql` | 평가 셋/케이스 테이블, unique/index 제약 추가 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/api/admin/evaluation/**` | UseCase, command, query, response DTO 계약 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationSet.java` | 평가 셋 엔티티와 상태 전이 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationCase.java` | 평가 케이스 엔티티와 검토 상태 전이 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationSetRepository.java` | 평가 셋 JPA 조회 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationCaseRepository.java` | 평가 케이스 JPA 조회 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationService.java` | 평가 셋/케이스 생성, 조회, 전이, asset candidate 생성, audit 기록 |
| Modify | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/web/AdminAiAuthentication.java` | 평가 관리/검토 권한 helper 추가, 기존 한국어 Javadoc과 Lead 결정 기록 보존 |
| Create | `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/web/AdminAiEvaluationController.java` | 관리자 평가 API endpoint 추가 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiEvaluationServiceTest.java` | 도메인 단위 테스트 |
| Test | `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/web/AdminAiEvaluationControllerTest.java` | 관리자 API controller 테스트 |

## 구현 순서

1. 기준 문서와 기존 `admin-server` AI 도메인 패턴을 확인한다.
2. `feature/ai-evaluation-cases` 브랜치에서 작업 상태를 확인한다.
3. 테스트를 먼저 추가해 평가 셋/케이스 계약, 권한, 상태 전이, audit 기대값을 고정한다.
4. `V35__create_ai_evaluation_tables.sql` 마이그레이션을 추가한다.
5. evaluation set/case 엔티티, enum, repository, service를 구현한다.
6. 관리자 web controller와 인증 helper를 추가한다.
7. asset 기반 candidate 생성 API를 마지막에 연결한다.
8. Claude 리뷰 기준 차단 항목을 반영한다.
   - 기존 한국어 Javadoc과 `2026-06-05 Lead 결정` 기록 복원
   - `reviewReason` audit snapshot 포함
   - approve/reject 권한을 `REVIEWER/SUPER_ADMIN`으로 축소
   - unique race 방어용 `DataIntegrityViolationException` 매핑
   - 권한 부정 경로와 public method 테스트 보강
   - 미인증 `Authentication` null 경로 컨트롤러 테스트 추가
9. 지정 테스트와 전체 `admin-server:test`를 실행한다.
10. 구현 커밋과 리뷰 대응 커밋을 작성한다.
11. 2차 리뷰 대응으로 인증 문서 복원과 미인증 MockMvc 테스트 보강을 확인하고 지정 테스트를 재실행한다.

## 테스트 보강

| 테스트 | 검증 내용 |
| --- | --- |
| `AiEvaluationServiceTest` | set 생성, 중복 version, 목록/상세 조회, activate 최소 10건 조건, retire 전이 |
| `AiEvaluationServiceTest` | case 생성, 목록/상세 조회, approve/reject 전이, 이미 처리된 case 재처리 실패 |
| `AiEvaluationServiceTest` | approve/reject audit action과 `reviewReason` snapshot 포함 |
| `AiEvaluationServiceTest` | CONTENT_CREATOR의 approve/reject 거부 |
| `AiEvaluationServiceTest` | asset candidate 생성 시 원문/민감 payload 미복사 |
| `AdminAiEvaluationControllerTest` | 명세 endpoint의 status code와 envelope |
| `AdminAiEvaluationControllerTest` | CONTENT_CREATOR 조회/생성 가능, REVIEWER approve/reject 가능 |
| `AdminAiEvaluationControllerTest` | 일반 USER, principal 없는 미인증 요청(401/M0002), CONTENT_CREATOR approve/reject 거부 |

## 수용 기준

- [x] 관리자가 평가 셋을 생성/조회할 수 있다.
- [x] 관리자가 평가 케이스를 생성/조회할 수 있다.
- [x] 케이스 approve/reject 상태 전이가 된다.
- [x] approve/reject audit log가 남는다.
- [x] approve/reject audit snapshot에 검토 사유가 남는다.
- [x] asset payload 기반 candidate 생성이 가능하다.
- [x] asset candidate 생성 시 prompt 원문, provider raw response, secret, 검증 주석 원문을 저장하지 않는다.
- [x] 일반 USER, 미인증, CONTENT_CREATOR approve/reject 권한 부정 경로가 controller/service 테스트로 검증된다.

## Subagent Decision

이 작업은 subagent 분산을 사용하지 않는다. 평가 셋, 평가 케이스, asset candidate가 같은 AI 도메인 파일과 같은 테스트 fixture를 공유하고, 리뷰 대응도 권한/audit/테스트가 한 흐름으로 얽혀 있어 단일 작업자가 순차적으로 처리하는 편이 충돌 위험이 낮다.

## 검증 명령

```powershell
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AiEvaluation*"
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AdminAi*ControllerTest"
.\qtai-server\gradlew.bat -p qtai-server :admin-server:test
```

## 실제 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AiEvaluation*"` | 성공, `0f074ec` 리뷰 대응 후 재실행 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AdminAi*ControllerTest"` | 성공, `0f074ec` 리뷰 대응 후 재실행 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test` | 성공 |

## 후속 작업

- OpenAPI 문서 반영
- 필요 시 `AiEvaluationService`를 Set/Case/Asset candidate 단위로 분리
- 필터 조합이 더 늘어나면 Specification 또는 QueryDSL 기반 동적 조회로 전환
- PR 본문에 관련 F-ID, workflow/report 경로, migration 버전 `V35`를 명시
