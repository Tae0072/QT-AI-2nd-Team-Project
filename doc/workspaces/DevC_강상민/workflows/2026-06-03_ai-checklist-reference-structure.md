# Workflow - 2026-06-03 ai-checklist-reference-structure

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-checklist-content-storage` |
| PR 대상 | `dev` |
| 관련 F-ID | 해당 없음 |
| 트리거 | `ai_validation_checklist_versions`, `validation_reference_jobs`, layer 2 검수 AI의 역할이 혼동되어 구조 정리가 필요함 |
| 기준 문서 | `AGENTS.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `07_요구사항_정의서.md`, `23_도메인_용어사전.md` |
| 대상 경로 | `doc/workspaces/DevC_강상민/workflows/2026-06-03_ai-checklist-reference-structure.md` |

## 작업 목표

체크리스트 version과 검증 참조자료의 책임을 문서로 고정한다. `ai_validation_checklist_versions`는 PDF 원문이나 해설 본문 저장소가 아니라, 외부 SSoT 기준 자료의 버전/해시/상태를 추적하는 registry이다.

성경 범위별 해설 PDF 원문은 외부 SSoT로 유지한다. 서버는 원문 전체를 저장하지 않고, 참조자료 처리 이력은 `validation_reference_jobs`, 실제 검수 결과는 `ai_validation_logs`에 기록한다.

## 현재 구조 정리

- `ai_validation_checklist_versions`는 `id`, `checklistType`, `version`, `contentHash`, `status`, `createdByAdminId`, `createdAt`, `activatedAt`, `retiredAt`만 저장한다.
- `checklistVersionId`는 승인 요청 입력값이 아니라, 검증 로그가 어떤 기준 버전으로 생성됐는지 추적하는 FK이다.
- `ai_prompt_versions`도 현재 프롬프트 본문 저장소가 아니라 `promptType`, `version`, `contentHash`, `status` 중심의 registry이다.
- 현재 layer 2 검수 AI 프롬프트는 `AiReviewValidationService` 코드의 `systemPrompt()`와 `userPrompt()`에서 조립한다.
- 현재 layer 2 `userPrompt()`에는 산출물 payload와 checklist version metadata만 들어가며, PDF 범위별 해설 참고자료 주입은 아직 구현되어 있지 않다.

## 설계 결정

- 외부 SSoT: 성경 범위별 해설 PDF 원문과 원본 파일은 서버 DB 원문 저장 대상이 아니다.
- Checklist registry: `ai_validation_checklist_versions`는 기준 자료 버전과 해시를 관리한다.
- Reference job: `validation_reference_jobs`는 PDF 파일명, 원문 해시, 제한 저장소 URI, 인덱스 URI, 상태를 추적한다.
- Validation log: `ai_validation_logs`는 `checklistVersionId`와 `validationReferenceJobId`를 함께 기록해 “어떤 기준 버전과 어떤 참조 작업으로 검수했는지”를 남긴다.
- Prompt source: 검수 AI에 보내는 system/user prompt 문장은 현재 코드에서 조립하며, 후속 구현에서 PDF 범위별 참고자료 조회 결과를 `userPrompt()`에 추가한다.

## 범위

- 체크리스트 version에 실제로 저장되는 값과 저장하지 않는 값을 명확히 정리한다.
- PDF 해설 자료가 `validation_reference_jobs`와 어떤 관계인지 정리한다.
- layer 2 검수 AI가 최종적으로 사용해야 할 데이터 흐름을 정리한다.
- 후속 구현 범위를 분리해 PR/작업 단위가 커지지 않도록 한다.

## 제외 범위

- DB schema 변경
- PDF 업로드, 파싱, 임베딩, 인덱싱 구현
- 성경 범위와 PDF 해설 범위 매칭 구현
- layer 2 검수 AI prompt 주입 코드 변경
- 관리자 API 또는 OpenAPI 변경
- `ai_validation_checklist_versions`에 원문 본문, prompt 본문, PDF 전문 저장 컬럼 추가

## 데이터 흐름 정리

1. 관리자는 외부 SSoT PDF/문서 기준으로 `ai_validation_checklist_versions`에 기준 버전과 `contentHash`를 등록한다.
2. 시스템 배치 또는 운영 도구는 PDF 참조자료 처리 결과를 `validation_reference_jobs`에 등록한다.
3. layer 1 AUTO 검증이 `PASSED`된 EXPLANATION 산출물에 대해 layer 2 ADVISOR 검수 AI가 실행된다.
4. 후속 구현에서는 산출물의 성경 범위를 기준으로 `validation_reference_jobs.indexStorageUri`에서 해당 범위의 해설 참고자료를 찾는다.
5. 검수 AI에는 생성 산출물, checklist version metadata, 범위별 참고자료 요약 또는 excerpt를 함께 보낸다.
6. 검수 결과는 `ai_validation_logs`에 `layer = 2`, `reviewerType = ADVISOR`, `checklistVersionId`, `validationReferenceJobId`, `result`로 저장한다.
7. `checklistJson`에는 판정 요약과 메타데이터만 저장하고, PDF 원문 전체나 긴 해설 본문은 저장하지 않는다.

## 용어 구분

| 용어 | 의미 | 저장 위치 |
| --- | --- | --- |
| 체크리스트 버전 | 검수 기준 자료의 버전/해시/상태 registry | `ai_validation_checklist_versions` |
| 체크리스트 원문 | 외부 SSoT 문서/PDF의 실제 내용 | 서버 DB 저장 안 함 |
| 검증 참조자료 작업 | PDF 원문/인덱스 처리 이력 | `validation_reference_jobs` |
| 검수 로그 | 실제 검증 결과와 사용한 기준 추적 | `ai_validation_logs` |
| 검수 AI 프롬프트 | LLM에 전달하는 system/user prompt | 현재 코드에서 조립 |

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 작업은 코드 구현이 아니라 구조 문서 정리이다.
- 핵심 결정은 `ai_validation_checklist_versions`, `validation_reference_jobs`, `ai_validation_logs`의 역할 구분으로 하나의 문서에서 일관되게 다루는 편이 안전하다.
- 병렬 작업으로 나누면 같은 용어를 다르게 설명할 위험이 크다.

### 위임 가능 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 기존 문서와 코드를 확인한 뒤 workflow spec을 직접 작성한다.

## 검증 계획

- `git status --short --branch`로 브랜치와 변경 파일을 확인한다.
- 작성 문서에 placeholder marker나 불완전한 결정 표현이 없는지 확인한다.
- 문서 작업만 수행하므로 Gradle 테스트는 실행하지 않는다.

## 후속 작업으로 남길 항목

- `validation_reference_jobs`와 활성 checklist version의 연결 정책 확정
- PDF 범위별 해설 추출/인덱싱 방식 설계
- 산출물 target range와 PDF 해설 range 매칭 방식 설계
- `AiReviewValidationService.userPrompt()`에 범위별 참고자료를 주입하는 구현
- layer 2 검수 로그에 `validationReferenceJobId`를 실제로 채우는 구현
- OpenAPI와 프로젝트 문서에서 registry/reference/log 역할 설명 보강
