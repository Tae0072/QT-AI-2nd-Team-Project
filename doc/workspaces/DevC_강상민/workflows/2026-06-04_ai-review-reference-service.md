# Workflow - 2026-06-04 ai-review-reference-service

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-review-reference-service` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | layer 2 검수 AI가 `validation_reference_jobs` 참조자료를 prompt/log에 연결하지 못하고 있어 참조자료 조회 통로가 필요함 |
| 기준 문서 | `AGENTS.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `07_요구사항_정의서.md`, `23_도메인_용어사전.md` |
| 대상 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/internal/**`, `qtai-server/src/test/java/com/qtai/domain/ai/internal/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

layer 2 검수 AI가 최신 ACTIVE `validation_reference_jobs`를 참조자료 metadata로 사용하도록 내부 조회 통로를 만든다. 검수 AI prompt에는 참조 작업 metadata를 포함하고, layer 2 검증 로그에는 `validationReferenceJobId`를 남긴다.

v1에서는 PDF 원문 파싱, 인덱스 조회, 성경 범위별 excerpt 생성을 구현하지 않는다. 서버는 PDF 원문을 DB에 저장하지 않고, 참조 작업 metadata만 검수 흐름에 연결한다.

## 범위

- `AiReviewReferenceService`를 추가해 최신 ACTIVE validation reference job을 조회한다.
- `ValidationReferenceJobRepository`에 최신 ACTIVE 조회 메서드를 추가한다.
- `AiReviewValidationService`가 참조 metadata를 prompt와 validation log에 연결한다.
- 참조 작업이 없으면 LLM 호출 없이 layer 2 `ADVISOR/NEEDS_REVIEW` 로그를 남긴다.
- focused unit test와 report를 작성한다.

## 제외 범위

- DB schema 변경
- 관리자 API 또는 OpenAPI 변경
- PDF 업로드, 파싱, 임베딩, 인덱스 조회 구현
- 산출물 성경 범위와 PDF 해설 범위 매칭 구현
- PDF 원문, 긴 excerpt, provider raw response, prompt 원문, secret/token/password/privateKey 저장

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Create | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewReferenceService.java` | 최신 ACTIVE 참조 작업 metadata 조회 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/ValidationReferenceJobRepository.java` | 최신 ACTIVE 조회 메서드 추가 |
| Modify | `qtai-server/src/main/java/com/qtai/domain/ai/internal/AiReviewValidationService.java` | prompt/log에 reference metadata 연결, 참조 없음 처리 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceServiceTest.java` | 최신 ACTIVE 선택과 empty 결과 검증 |
| Test | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewValidationServiceTest.java` | prompt/log 연결과 참조 없음 NEEDS_REVIEW 검증 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-04_ai-review-reference-service_report.md` | 실행 결과, 검증, 후속 작업 기록 |

## 구현 순서

1. workflow spec을 저장한다.
2. `workflow-spec-runner` 절차로 spec을 다시 읽고 직접 실행을 선택한다.
3. `AiReviewReferenceServiceTest`를 먼저 작성하고 실패를 확인한다.
4. `AiReviewReferenceService`와 repository 메서드를 최소 구현한다.
5. `AiReviewValidationServiceTest`에 reference metadata prompt/log 연결 테스트와 참조 없음 테스트를 추가하고 실패를 확인한다.
6. `AiReviewValidationService`를 수정해 테스트를 통과시킨다.
7. focused test를 실행한다.
8. 가능하면 전체 test를 실행한다.
9. report를 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `AiReviewReferenceServiceTest` | 최신 ACTIVE reference job을 `createdAt desc`, `id desc` 기준으로 선택 |
| `AiReviewReferenceServiceTest` | ACTIVE job이 없으면 empty 결과 반환 |
| `AiReviewReferenceServiceTest` | EXPIRED/DELETED job은 선택하지 않음 |
| `AiReviewValidationServiceTest` | reference metadata가 LLM userPrompt에 포함됨 |
| `AiReviewValidationServiceTest` | layer 2 로그에 `validationReferenceJobId` 저장 |
| `AiReviewValidationServiceTest` | reference job이 없으면 LLM 미호출, `NEEDS_REVIEW`, `AI_REVIEW_REFERENCE_NOT_FOUND` 로그 저장 |
| `AiReviewValidationServiceTest` | `checklistJson`에 reference metadata만 저장하고 원문/secret/token/password/raw response 저장 금지 |

## 수용 기준

- [ ] `AiReviewReferenceService`가 최신 ACTIVE reference job metadata를 반환한다.
- [ ] ACTIVE reference job이 없으면 layer 2 검수 AI는 LLM을 호출하지 않는다.
- [ ] 참조 작업 없음 경로는 layer 2 `ADVISOR/NEEDS_REVIEW` 로그와 `AI_REVIEW_REFERENCE_NOT_FOUND`를 남긴다.
- [ ] 참조 작업 있음 경로는 prompt에 reference metadata를 포함하고 layer 2 로그에 `validationReferenceJobId`를 저장한다.
- [ ] PDF 원문, 긴 excerpt, provider raw response, prompt 원문, secret/token/password/privateKey를 저장하지 않는다.
- [ ] DB schema, 관리자 API, OpenAPI를 변경하지 않는다.
- [ ] report를 작성한다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 대상이 `AiReviewValidationService`, 신규 reference service, 관련 unit test로 강하게 연결되어 있다.
- TDD red/green 순서를 한 흐름에서 확인해야 한다.
- 병렬 분리 시 같은 테스트 fixture와 생성자 시그니처 변경이 겹칠 수 있다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow spec을 기준으로 TDD, 구현, focused 검증, report 작성을 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\Users\HSystem\.jdks\temurin-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*AiReviewReferenceServiceTest" --tests "*AiReviewValidationServiceTest"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test
```

전체 test가 환경 문제나 시간 문제로 실패하면 focused test 결과와 실패 사유를 report에 남긴다.

## 후속 작업으로 남길 항목

- PDF 범위별 해설 추출/인덱스 조회 구현
- 산출물 target range와 PDF 해설 range 매칭 구현
- reference excerpt 또는 summary를 prompt에 주입하는 후속 PR
- `validation_reference_jobs`와 checklist version의 명시적 연결 정책 검토
