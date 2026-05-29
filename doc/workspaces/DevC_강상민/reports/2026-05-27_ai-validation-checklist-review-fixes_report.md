# 2026-05-27 AI 검증 체크리스트 리뷰 보정 리포트

## 기준

- Workflow: `doc/workspaces/DevC_강상민/workflows/2026-05-27_ai-validation-checklist-review-fixes.md`
- 관련 PR: `feature/ai-validation-checklist-versions-api` -> `dev`
- 관련 F-ID: F-14
- 트리거: Claude 리뷰 BLOCK/WARN 보정

## 반영 내용

- `findAllByFilters()` JPQL null enum 조건을 제거하고, service에서 필터 조합별 repository 메서드로 분기하도록 변경했다.
- 활성화 경로에서 대상 id의 `checklistType`만 scalar 쿼리로 확인한 뒤, 같은 `checklistType` row 전체를 `PESSIMISTIC_WRITE`로 조회해 기존 ACTIVE 자동 폐기와 target 활성화를 한 트랜잭션 안에서 처리하도록 보강했다.
- Controller는 생성 요청의 `status` null/blank만 `DRAFT`로 보정하고, `ACTIVE/RETIRED` 차단은 service 비즈니스 검증이 담당하도록 책임을 정리했다.
- Controller/Service 테스트에 `CONTENT_CREATOR` 차단 케이스를 추가했다.
- API 명세서에서 현재 구현과 Java `ErrorCode`에 없는 `ACTIVE_CHECKLIST_EXISTS` 표기를 제거했다.

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `.\gradlew.bat test --tests "*AiValidationChecklistVersionRepositoryTest" --tests "*AdminAiValidationChecklistServiceTest" --tests "*AdminAiValidationChecklistControllerTest"` | PASS |
| `.\gradlew.bat test --tests "*Ai*"` | PASS |
| `.\gradlew.bat build` | PASS |
| `npx.cmd @apidevtools/swagger-cli validate qtai-server/apis/api-v1/openapi.yaml` | PASS |
| `rg -n "ACTIVE_CHECKLIST_EXISTS" "qtai-server/src/main/java" "qtai-server/apis/api-v1/openapi.yaml" "doc/프로젝트 문서/04_API_명세서.md"` | PASS: 매칭 없음 |
| `rg -n "findAllByFilters" qtai-server/src/main/java qtai-server/src/test/java` | PASS: 매칭 없음 |
| `rg -n "^import .*domain\\.[a-z]+\\.(internal\|web\|repository)" qtai-server/src/main/java/com/qtai/domain/ai` | PASS: 매칭 없음 |

## 미실행 항목

- `npx @stoplight/spectral-cli lint ... --ruleset .spectral.yaml`: 저장소 루트에 `.spectral.yaml`이 없어 로컬 실행 불가.
- `.\gradlew.bat test jacocoTestReport`, `.\gradlew.bat jacocoTestCoverageVerification`: 현재 Gradle task 목록에 해당 task가 없어 로컬 실행 불가.
- `gitleaks detect --source . --redact --exit-code 1`: 로컬 PATH에서 `gitleaks` 실행 파일을 찾을 수 없어 실행 불가.

## 후속 권장

- `AdminAiAssetController`의 인라인 관리자 인증 로직을 후속 PR에서 `AdminAiAuthentication`으로 통합한다.
- 감사 로그가 비즈니스 트랜잭션 실패와 분리되어야 한다면 `REQUIRES_NEW` 또는 이벤트 기반 처리로 별도 검토한다.
- 운영 DB 수준에서 ACTIVE 단일성을 더 강하게 보장해야 한다면 MySQL 제약 또는 Testcontainers 기반 동시성 통합 테스트를 별도 설계한다.
