# Workflow — 2026-05-21 ai-failure-retry-policy

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-failure-retry-policy` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 트리거 | 2026-05-21 DevC 일정의 "실패·재처리 기준 정리" 작업 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 해당 경로 | 아래 해당 경로 목록 참고 |

## 기준 문서

- `doc/workspaces/DevC_강상민/강상민_공식일정표.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-generation-log-model.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-generation-log-model-review-fixes.md`
- `doc/workspaces/DevC_강상민/workflows/2026-05-20_commentary-validation-flow-policy.md`
- `doc/프로젝트 문서/03_아키텍처_정의서.md`
- `doc/프로젝트 문서/04_API_명세서.md`
- `doc/프로젝트 문서/05_시퀀스_다이어그램.md`
- `doc/프로젝트 문서/07_요구사항_정의서.md`
- `doc/프로젝트 문서/18_코드_품질_게이트.md`
- `doc/프로젝트 문서/23_도메인_용어사전.md`
- `doc/프로젝트 문서/25_기능_명세서.md`

## 해당 경로

- `doc/workspaces/DevC_강상민/**`
- `doc/프로젝트 문서/**`
- `qtai-server/src/main/java/com/qtai/domain/ai/**`
- `qtai-server/src/test/java/com/qtai/domain/ai/**`
- `qtai-server/src/main/java/com/qtai/domain/admin/**`

## 작업 목표

이번 작업은 W2의 AI 생성 골격과 관리자 트리거 구현 전에 실패·재처리 기준을 문서 기준으로 고정하는 사전 명세 작업이다. `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`가 실패 상태, 재처리 가능 근거, 관리자 재생성 판단에 필요한 정보를 일관되게 남기도록 기준을 정리한다.

특히 핸들러 실패 로그, 중복 실행 방지, 관리자 재생성 기준을 분리해 정의한다. 사용자 요청 경로에서 해설·시뮬레이터 생성이 시작되지 않도록 유지하고, 실패 로그에도 검증 참조 원문, provider raw response, prompt 원문이 섞이지 않게 한다.

## 범위

- `ai_generation_jobs.status`의 실패·종료 상태 의미를 정리한다.
- `ai_generated_assets.status`와 `ai_validation_logs.result`가 재처리 판단에 어떻게 쓰이는지 정리한다.
- 핸들러 실패 로그에 남길 필수 필드를 정리한다.
- 동일 target/prompt/input 기준 중복 실행 방지 정책을 정리한다.
- 관리자 재생성 요청이 가능한 상태와 불가능한 상태를 구분한다.
- 관리자 재생성 시 기존 job/asset/log를 덮어쓰지 않고 새 이력으로 추적하는 기준을 정리한다.
- 실패 사유는 사용자 노출 메시지와 운영 로그 메시지로 분리한다.
- W2 구현자가 추가해야 할 단위 테스트와 경계 테스트 기준을 정리한다.

## 제외 범위

- 실제 배치 스케줄러와 관리자 재생성 API 구현은 하지 않는다.
- 실제 DeepSeek 또는 외부 LLM 호출 구현은 하지 않는다.
- DB migration, Entity, Repository, Service 코드는 이번 작업에서 수정하지 않는다.
- 관리자 화면, 운영 대시보드, 알림 UI는 이번 작업에서 구현하지 않는다.
- 실패 로그에 검증 참조 원문, provider raw response, prompt 원문을 포함하지 않는다.
- 완전 무손실을 보장하는 표현은 사용하지 않는다.

## 정책 결정 초안

| 구분 | 기준 |
| --- | --- |
| 생성 작업 실패 | `ai_generation_jobs.status=FAILED`와 실패 사유를 남긴다. |
| 산출물 검증 실패 | `ai_validation_logs.result=REJECTED`를 남기고 산출물은 사용자 노출 상태로 전환하지 않는다. |
| 검토 필요 | `NEEDS_REVIEW`는 관리자 검토 대상으로 유지하고 자동 승인하지 않는다. |
| 재처리 가능 | `FAILED`, `REJECTED`, `NEEDS_REVIEW` 중 원인과 target 정보가 남은 경우 관리자 또는 배치 재처리 후보가 된다. |
| 중복 실행 방지 | 동일 target type/id, asset type, prompt version, input hash 기준으로 `QUEUED` 또는 `RUNNING` 작업이 있으면 새 작업 생성을 차단한다. |
| 관리자 재생성 | 기존 asset/log를 수정 삭제하지 않고 새 generation job과 새 asset을 만든다. |
| 기존 승인본 | 새 산출물이 승인되기 전까지 기존 `APPROVED`/`ACTIVE` 노출본은 유지한다. |
| 로그 금지 항목 | provider raw response, 검증 참조 원문, secret, prompt 원문은 저장하지 않는다. |

## 실패·재처리 세부 기준

### 핸들러 실패 로그 필수 필드

| 필드 | 기준 |
| --- | --- |
| `eventId` | 이벤트 식별자. 재처리와 추적의 1차 기준으로 남긴다. |
| `eventType` | 실패한 이벤트 종류를 남긴다. |
| `handlerName` | 실패한 핸들러 이름을 남긴다. |
| `errorMessage` | 운영자가 원인을 판단할 수 있는 요약 메시지만 남긴다. |
| `jobId` | AI 생성 작업과 연결된 실패면 `ai_generation_jobs.id`를 남긴다. |
| `targetType`, `targetId` | 재처리 대상을 찾을 수 있도록 target 정보를 남긴다. |
| `assetType` | 산출물 생성 이후 실패면 대상 산출물 유형을 남긴다. |
| `promptVersion` | 동일 입력 재처리와 회귀 분석을 위해 사용한 지시 버전을 남긴다. |
| `inputHash` | 중복 실행 방지를 위한 후보 키로 남긴다. 현재 구현에는 저장 위치가 없으므로 W2에서 컬럼 또는 내부 로그 저장 위치를 확정해야 한다. |

실패 로그에는 provider raw response, 검증 참조 원문, prompt 원문, secret, token, password, private key를 남기지 않는다.

### 중복 실행 방지 기준

| 구분 | 기준 |
| --- | --- |
| 논리 키 | `targetType + targetId + assetType + promptVersion + inputHash` |
| 차단 상태 | 동일 논리 키의 `QUEUED` 또는 `RUNNING` 작업이 있으면 새 작업 생성을 차단한다. |
| 허용 상태 | 기존 작업이 `SUCCEEDED`, `FAILED`, `REJECTED`, `HIDDEN`에 대응되는 완료 상태면 관리자 재생성 정책에 따라 새 작업을 만들 수 있다. |
| 현재 gap | `inputHash`는 용어사전에 정의되어 있으나 현재 ERD/코드에는 물리 저장 위치가 없다. W2 구현에서 저장 위치와 유니크/조회 인덱스를 결정해야 한다. |

### 관리자 재생성 기준

| 상태 | 재생성 가능 여부 | 기준 |
| --- | --- | --- |
| `FAILED` job | 가능 | target 정보와 실패 사유가 남아 있으면 새 generation job을 만든다. |
| `REJECTED` asset/log | 가능 | 반려 사유를 유지하고 새 generation job과 새 asset을 만든다. |
| `NEEDS_REVIEW` log | 가능 | 운영 판단으로 재생성할 수 있으나 자동 승인으로 전환하지 않는다. |
| `VALIDATING` asset | 불가 | 검증 중인 작업을 덮어쓰지 않는다. |
| `QUEUED`/`RUNNING` job | 불가 | 중복 실행 방지 대상이다. |
| `APPROVED`/`ACTIVE` 노출본 | 조건부 가능 | 기존 노출본을 유지한 채 새 이력으로 재생성한다. 새 산출물이 승인되기 전까지 기존 노출본을 교체하지 않는다. |

## 구현 순서

1. DevC 공식 일정표의 2026-05-21 작업과 W2 작업의 관계를 확인한다.
2. 5/20 AI 생성·검증 로그 workflow와 review-fixes workflow의 상태 전이 기준을 확인한다.
3. 요구사항·기능 명세에서 실패 로그와 재처리 가능 상태의 수용 기준을 확인한다.
4. 아키텍처·시퀀스 문서에서 관리자 승인, 관리자 재생성, 사용자 노출본 연결 흐름을 확인한다.
5. ERD 기준으로 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`에 남길 필드와 상태값을 대조한다.
6. 중복 실행 방지 키 후보를 `targetType`, `targetId`, `assetType`, `promptVersion`, `inputHash` 기준으로 정리한다.
7. 관리자 재생성 가능/불가능 상태 표와 후속 테스트 기준을 작성한다.

## 테스트 보강 목록

| 테스트 파일 | 추가할 검증 |
| --- | --- |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiGenerationJobTest.java` | terminal 상태에서 성공/실패 상태로 다시 전이되지 않는지 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | 생성 실패 시 target 정보와 실패 사유가 남고 재처리 판단이 가능한지 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/internal/AiLogServiceTest.java` | `PASSED`, `NEEDS_REVIEW`, `REJECTED`별 asset 상태 정책이 자동 승인으로 흐르지 않는지 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/**` | 동일 target/prompt/input 기준 `QUEUED` 또는 `RUNNING` 작업 중복 생성 차단 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/**` | 관리자 재생성 시 기존 job/asset/log를 덮어쓰지 않고 새 이력으로 남기는지 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/**` | 실패 로그와 checklist/payload에 provider raw response, 검증 참조 원문, secret이 저장되지 않는지 검증 |

## 수용 기준

- [ ] 5/21 작업이 W2 구현 전 실패·재처리 기준 명세 작업임이 명확하다.
- [ ] 핸들러 실패 로그 필수 필드가 정리된다.
- [ ] `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`의 실패·검토·반려 상태 의미가 정리된다.
- [ ] 중복 실행 방지 기준이 target, asset, prompt, input 단위로 정리된다.
- [ ] 관리자 재생성 가능 상태와 불가능 상태가 구분된다.
- [ ] 관리자 재생성 시 기존 이력을 보존하고 새 이력을 추가하는 기준이 정리된다.
- [ ] 검증 참조 원문, provider raw response, prompt 원문을 로그에 남기지 않는 기준이 유지된다.
- [ ] W2 구현 시 추가할 테스트 기준이 구체화된다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 작업은 코드 구현보다 문서와 기존 로그 모델의 정책 해석을 일관되게 정리하는 것이 핵심이다.
- 편집 대상이 단일 workflow 문서에 가깝고, 상태 전이·재처리 기준은 한 사람이 문맥을 유지하며 정리하는 편이 안전하다.
- 실제 코드 구현은 W2에서 batch/admin 경로가 생긴 뒤 별도 workflow로 분리하는 편이 충돌을 줄인다.

### 위임 가능한 작업

| Worker | 위임 작업 | 편집 가능 경로 |
| --- | --- | --- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음 |

### 직접 실행 판단

메인 에이전트가 기준 문서 확인, 실패·재처리 정책 정리, workflow 작성, 최종 검증을 직접 수행한다.

## 검증 계획

- `rg -n "실패|재처리|중복 실행|관리자 재생성|ai_generation_jobs|ai_generated_assets|ai_validation_logs" doc qtai-server`로 관련 기준을 확인한다.
- `doc/workspaces/DevC_강상민/강상민_공식일정표.md`에서 5/21 작업과 W2 구현 범위가 충돌하지 않는지 확인한다.
- `doc/프로젝트 문서/18_코드_품질_게이트.md`에서 이벤트 실패 로그와 재처리 가능 기준을 확인한다.
- `doc/프로젝트 문서/23_도메인_용어사전.md`에서 `inputHash`, 핸들러 실패 로그, 재처리 가능 용어를 확인한다.
- 이번 workflow 작성 자체는 코드 변경이 아니므로 Gradle 테스트는 실행하지 않는다.

## 후속 작업으로 넘길 항목

- W2 `commentary_materials` → `verse_explanations` 생성 골격 구현
- batch/admin 생성 경로의 중복 실행 방지 구현
- 관리자 재생성 API 또는 UseCase 구현
- 관리자 승인 시 `verse_explanations.APPROVED` + `activeUniqueKey='ACTIVE'` 연결 정책 구현
- 실패 로그/재처리 후보 관리자 조회 화면 또는 API 설계
- 실패·재처리 기준에 대한 단위 테스트와 관리자 재생성 통합 테스트 보강
