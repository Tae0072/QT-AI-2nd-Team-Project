# Workflow - 2026-06-05 ai-review-reference-system-api-index-storage-uri-flow

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `test/ai-reference-system-api-index-storage-uri-flow` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `validation_reference_jobs.indexStorageUri`가 system API 실제 호출 경로에서도 최종 URI로 저장되고 최신 reference metadata로 조회되는지 검증해야 한다. |
| 기준 문서 | `AGENTS.md`, `doc/workspaces/DevC_강상민/workflows/2026-05-28_ai-validation-reference-jobs.md`, `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-index-storage-uri-flow.md` |
| 대상 경로 | `qtai-server/src/test/java/com/qtai/domain/ai/**`, `doc/workspaces/DevC_강상민/workflows/**`, `doc/workspaces/DevC_강상민/reports/**` |

## 작업 목표

`POST /api/v1/system/validation-reference-jobs` 실제 Spring MockMvc 호출이 `indexStorageUri = restricted://validation/index/reference-index.json`를 DB에 저장하고, 같은 값을 최신 ACTIVE reference metadata로 조회하는지 통합 테스트로 고정한다.

이번 작업은 실제 운영 system token 또는 service account 검증이 아니라, 현재 코드의 Spring Security test principal과 실제 controller/service/repository/H2 흐름을 통과하는 system API 호출 검증이다.

## 범위

- 기존 standalone controller test fixture의 `indexStorageUri`를 최종 URI로 갱신한다.
- Spring Boot + MockMvc 통합 테스트를 추가한다.
- system API POST 호출 후 응답에 민감 URI/hash가 노출되지 않는지 검증한다.
- POST 호출 후 DB row의 `indexStorageUri`, `sourceFileHash` 저장을 검증한다.
- 같은 DB row가 `AiReviewReferenceService.latestActiveReference()` metadata로 조회되는지 검증한다.
- workflow/report를 작성한다.

## 제외 범위

- 생산 코드 변경
- DB schema, OpenAPI, API 응답 필드 변경
- 로컬 서버 실행 후 curl 검증
- 운영 system token/service account 구현 또는 검증
- 운영 restricted storage 배포
- 실제 IVP 원문 JSON, PDF, `qtai-server/restricted/**`, `qtai-server/build/**` 커밋

## 파일 구조와 책임

| 구분 | 경로 | 책임 |
| --- | --- | --- |
| Modify | `qtai-server/src/test/java/com/qtai/domain/ai/web/SystemValidationReferenceJobControllerTest.java` | standalone controller request/command fixture를 최종 URI로 갱신 |
| Create | `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiReviewReferenceSystemApiFlowIntegrationTest.java` | system API POST -> DB row -> latest metadata 통합 검증 |
| Create | `doc/workspaces/DevC_강상민/workflows/2026-06-05_ai-review-reference-system-api-index-storage-uri-flow.md` | 작업 명세 기록 |
| Create | `doc/workspaces/DevC_강상민/reports/2026-06-05_ai-review-reference-system-api-index-storage-uri-flow_report.md` | 실행 결과와 검증 기록 |

## 구현 순서

1. `dev` 최신 상태에서 `test/ai-reference-system-api-index-storage-uri-flow` 브랜치를 생성한다.
2. workflow spec을 저장한다.
3. `SystemValidationReferenceJobControllerTest`의 request fixture와 command assertion을 `restricted://validation/index/reference-index.json`로 갱신한다.
4. `AiReviewReferenceSystemApiFlowIntegrationTest`를 추가한다.
5. 신규 통합 테스트는 `@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@Transactional`을 사용한다.
6. 신규 통합 테스트는 `with(user("batch").authorities(new SimpleGrantedAuthority("ROLE_SYSTEM_BATCH")))`로 인증된 system batch 요청을 만든다.
7. POST 응답에서 `sourceFileHash`, `storageUri`, `indexStorageUri`가 노출되지 않는지 검증한다.
8. POST 응답 id로 `ValidationReferenceJobRepository`를 조회해 DB 저장값을 검증한다.
9. `AiReviewReferenceService.latestActiveReference()`가 같은 id, source hash, index URI를 반환하는지 검증한다.
10. focused test를 실행한다.
11. 민감 산출물 ignored/staged 상태를 확인한다.
12. report를 작성하고 원문성 키워드가 없는지 확인한다.
13. commit message convention에 맞춰 커밋한다.

## 테스트 보강 목록

| 테스트 파일 | 추가 검증 |
| --- | --- |
| `SystemValidationReferenceJobControllerTest` | standalone request fixture와 command assertion이 최종 URI를 사용 |
| `AiReviewReferenceSystemApiFlowIntegrationTest` | system API POST가 controller/service/repository를 통과해 DB row에 최종 URI를 저장 |
| `AiReviewReferenceSystemApiFlowIntegrationTest` | API 응답에는 민감 URI/hash가 노출되지 않음 |
| `AiReviewReferenceSystemApiFlowIntegrationTest` | latest ACTIVE reference metadata가 최종 URI를 반환 |

## 수용 기준

- [ ] system API POST 호출이 `201 Created`를 반환한다.
- [ ] POST 응답에는 `sourceFileHash`, `storageUri`, `indexStorageUri`가 노출되지 않는다.
- [ ] DB row에 `restricted://validation/index/reference-index.json`가 저장된다.
- [ ] DB row의 `sourceFileHash`도 요청 값으로 저장된다.
- [ ] latest ACTIVE reference metadata가 같은 id, source hash, index URI를 반환한다.
- [ ] 생산 코드, DB schema, OpenAPI, API 응답 필드는 변경되지 않는다.
- [ ] focused test가 통과한다.
- [ ] 민감 산출물은 ignored 상태이며 staged 대상이 아니다.
- [ ] report에는 원문성 키워드가 포함되지 않는다.
- [ ] 커밋이 생성된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 변경 범위가 테스트와 문서에 한정되어 있다.
- system API, DB row, latest metadata가 같은 검증 흐름에 묶여 있어 직접 실행이 더 안전하다.
- 민감 산출물 stage 금지 확인을 마지막에 직접 수행해야 한다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 workflow, 테스트 보강, 검증, report, 커밋을 직접 수행한다.

## 검증 계획

```powershell
$env:JAVA_HOME='C:\TOOLS\jdk-21'
$env:PATH="$env:JAVA_HOME\bin;$env:PATH"
.\qtai-server\gradlew.bat --no-daemon -p qtai-server test --tests "*SystemValidationReferenceJobControllerTest" --tests "*AiReviewReferenceSystemApiFlowIntegrationTest" --tests "*ValidationReferenceJob*Test" --tests "*AiReviewReferenceServiceTest"
```

```powershell
git status --short --ignored -- qtai-server\restricted qtai-server\build\ai-review-reference doc\TalkFile_IVP성경배경주석.pdf.pdf
git diff --cached --name-only -- qtai-server\restricted qtai-server\build doc\TalkFile_IVP성경배경주석.pdf.pdf
rg -n "referenceText|excerpt|본문" doc\workspaces\DevC_강상민\reports\2026-06-05_ai-review-reference-system-api-index-storage-uri-flow_report.md
git diff --cached --check
```

## 후속 작업으로 남길 항목

- 실제 운영 system token/service account 검증
- 로컬 서버 실행 후 curl 방식 수동 검증
- 운영 환경 `QTAI_RESTRICTED_STORAGE_ROOT` 연결 검증
