# Workflow — 2026-05-20 ai-generation-log-model

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-generation-log-model` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14 |
| 기준 문서 | `07_요구사항_정의서.md` v3.1, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `23_도메인_용어사전.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

AI 생성·검증 흐름의 공통 기반이 되는 작업 로그 모델을 먼저 고정한다. 모든 해설, 시뮬레이터, F-15 Q&A 산출물은 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs` 기준으로 추적 가능해야 한다.

## 문제 정의

AI 기능은 사용자에게 결과만 보여주는 기능이 아니라 생성 지시 버전, 산출물 상태, 검증 체크리스트 버전, 실패 사유를 운영자가 추적할 수 있어야 한다. 로그 모델이 먼저 고정되지 않으면 이후 사전 생성, 관리자 승인, Q&A 검증 흐름이 서로 다른 방식으로 실패를 기록하게 된다.

## 범위

- `domain.ai.internal`에 AI 생성 작업, 산출물, 검증 로그 Entity와 enum을 추가한다.
- Repository는 같은 도메인의 `internal` 안에 둔다.
- 상태값은 API 명세와 용어사전 기준을 따른다.
- 실패 사유는 사용자 응답용 메시지와 운영 로그용 메시지를 분리한다.
- 외부 AI 원문, 검증 참조 원문, provider secret은 로그에 남기지 않는다.

## 제외 범위

- 실제 DeepSeek 호출 구현은 이 작업에서 하지 않는다.
- 관리자 API와 사용자 Q&A API는 이 작업에서 열지 않는다.
- `verse_explanations`, `simulator_clips` 최종 연결은 후속 작업에서 처리한다.
- Redis, queue, SSE, `/ai/sessions/**`는 도입하지 않는다.

## 데이터 기준

| 모델 | 역할 | 필수 추적값 |
| --- | --- | --- |
| `AiGenerationJob` | 생성 작업 단위 | job type, target type/id, prompt version, status, error message, started/finished time |
| `AiGeneratedAsset` | 생성 산출물 | asset type, target type/id, payload json, status, prompt version, source label |
| `AiValidationLog` | 검증 결과 | asset id, layer, result, reviewer type, checklist version id, checklist json, error message |

## 상태 기준

- `ai_generation_jobs.status`: `QUEUED`, `RUNNING`, `SUCCEEDED`, `FAILED`
- `ai_generated_assets.status`: `VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN`
- `ai_validation_logs.result`: `PASSED`, `REJECTED`, `NEEDS_REVIEW`
- `ai_validation_logs.reviewerType`: `AUTO`, `ADMIN`, `ADVISOR`

## 구현 순서

1. 현재 `domain.ai` 스켈레톤을 확인하고 기존 TODO 클래스와 충돌하지 않게 책임을 재배치한다.
2. Entity와 enum을 `domain.ai.internal`에 추가한다.
3. Repository 인터페이스를 추가한다.
4. 실패 사유 길이, `payloadJson`, `checklistJson`, `sourceLabel` 필드의 null 허용 기준을 정한다.
5. 생성 작업 시작, 성공, 실패, 산출물 등록, 검증 로그 등록을 담당하는 내부 Service 메서드를 만든다.
6. 단위 테스트로 상태 전이와 민감정보 미저장 기준을 검증한다.

## 수용 기준

- [ ] AI 작업은 생성 전 `QUEUED` 또는 `RUNNING` 상태로 기록된다.
- [ ] 생성 실패 시 `FAILED`와 실패 사유가 남고 재처리 가능한 target 정보가 유지된다.
- [ ] 검증 실패 산출물은 `APPROVED`가 되지 않는다.
- [ ] 검증 로그는 checklist version과 reviewer type을 추적한다.
- [ ] 로그와 테스트 fixture에 token, password, private key 예시가 없다.

## 검증 계획

- `./gradlew -p qtai-server test --tests "*Ai*"`
- `./gradlew -p qtai-server build`
- `gitleaks detect --source . --redact --exit-code 1`

## PR 전 체크

- [ ] `domain.ai` 밖의 `internal` 타입을 직접 import하지 않았다.
- [ ] Controller에서 Repository를 직접 호출하지 않는다.
- [ ] `jakarta.*`만 사용한다.
- [ ] 관련 F-ID(F-02, F-14)를 PR 설명에 남긴다.
