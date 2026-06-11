# Report — 2026-06-11 ai-evaluation-cases

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-evaluation-cases` |
| PR 대상 | `dev` |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-06-11_ai-evaluation-cases.md` |
| 관련 F-ID | F-14 |
| 작성 위치 | `doc/workspaces/DevC_강상민/reports/2026-06-11_ai-evaluation-cases_report.md` |

## 작업 결과

`admin-server`에 AI 평가 셋과 평가 케이스 관리 기능을 추가했다. 관리자는 평가 셋과 케이스를 생성/조회할 수 있고, 검토 권한이 있는 관리자는 케이스를 승인/반려할 수 있다. 승인/반려 결과는 감사 로그로 기록되며, asset payload 기반 candidate 생성 API도 연결했다.

## 변경 요약

1. `V35__create_ai_evaluation_tables.sql`로 `ai_evaluation_sets`, `ai_evaluation_cases` 테이블을 추가했다.
2. `(eval_type, version)` unique 제약과 조회용 index를 추가했다.
3. `domain.ai.api.admin.evaluation`에 평가 관리 UseCase와 command/query/response DTO를 추가했다.
4. `AiEvaluationSet`, `AiEvaluationCase`, enum, repository, `AiEvaluationService`를 추가했다.
5. `AdminAiEvaluationController`를 추가해 관리자 API를 노출했다.
6. `AdminAiAuthentication`에 평가 관리 권한 helper를 추가했다.
7. `EVAL_CASE_APPROVE`, `EVAL_CASE_REJECT` audit action 기록을 연결했다.
8. asset 기반 evaluation candidate 생성 API를 추가했다.
9. 서비스와 컨트롤러 테스트를 추가/보강했다.

## 주요 변경 파일

| 파일 | 내용 |
| --- | --- |
| `qtai-server/admin-server/src/main/resources/db/migration/V35__create_ai_evaluation_tables.sql` | 평가 셋/케이스 테이블과 제약 추가 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/api/admin/evaluation/**` | 평가 관리 공개 UseCase/DTO 추가 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationService.java` | 평가 셋/케이스 생성, 조회, 상태 전이, audit, asset candidate 생성 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationSet.java` | 평가 셋 엔티티 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/internal/AiEvaluationCase.java` | 평가 케이스 엔티티 |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/web/AdminAiEvaluationController.java` | 관리자 평가 API |
| `qtai-server/admin-server/src/main/java/com/qtai/domain/ai/web/AdminAiAuthentication.java` | 평가 관리/검토 권한 helper |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/internal/AiEvaluationServiceTest.java` | 평가 도메인 테스트 |
| `qtai-server/admin-server/src/test/java/com/qtai/domain/ai/web/AdminAiEvaluationControllerTest.java` | 관리자 평가 API 테스트 |

## 리뷰 대응

| 리뷰 항목 | 반영 |
| --- | --- |
| Javadoc 위치 오류 | `AdminAiAuthentication`의 평가 관리/모니터링 Javadoc 위치를 분리했다. |
| Flyway 마이그레이션 누락 지적 | `V35__create_ai_evaluation_tables.sql`이 포함되어 있음을 확인했다. |
| `reviewReason` 유실 | approve/reject audit after snapshot에 `reviewReason`을 포함했다. |
| 중복 검사 TOCTOU | DB unique 제약을 두고 `DataIntegrityViolationException`을 `DUPLICATE_RESOURCE`로 매핑했다. |
| 권한 부정 경로 테스트 누락 | USER/미인증/CONTENT_CREATOR approve-reject 거부 테스트를 추가했다. |
| approve/reject에 CONTENT_CREATOR 허용 | controller와 service 모두 `REVIEWER/SUPER_ADMIN`으로 축소했다. |
| public method 테스트 누락 | set/case 목록/상세/생성/retire/enum 실패 경로 테스트를 보강했다. |

## 권한 정책

| 작업 | 허용 권한 |
| --- | --- |
| 평가 셋 생성/조회/활성화/은퇴 | `ADMIN + CONTENT_CREATOR/REVIEWER/SUPER_ADMIN` |
| 평가 케이스 생성/조회 | `ADMIN + CONTENT_CREATOR/REVIEWER/SUPER_ADMIN` |
| 평가 케이스 approve/reject | `ADMIN + REVIEWER/SUPER_ADMIN` |

## 상태 전이

| 대상 | 전이 |
| --- | --- |
| 평가 셋 | `DRAFT -> ACTIVE -> RETIRED` |
| 평가 케이스 | `CANDIDATE -> APPROVED` 또는 `CANDIDATE -> REJECTED` |

평가 셋 활성화는 승인된 케이스가 최소 10건 이상일 때만 허용한다. 조건을 만족하지 못하면 `INVALID_STATUS_TRANSITION`으로 응답한다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AiEvaluation*"` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test --tests "*AdminAi*ControllerTest"` | 성공 |
| `.\qtai-server\gradlew.bat -p qtai-server :admin-server:test` | 성공 |

## 커밋

| 커밋 | 내용 |
| --- | --- |
| `9e12833` | `feat(ai): AI 평가 셋과 평가 케이스 관리 기능 추가` |
| `326aebc` | `fix(ai): 평가 케이스 검토 권한과 감사 로그 보강` |

## 남은 후속 작업

- OpenAPI 문서 반영
- PR 본문에 F-ID, workflow/report 경로, migration 버전 `V35` 명시
- 필요 시 `AiEvaluationService` 책임 분리와 동적 조회 리팩터링을 별도 PR로 진행
