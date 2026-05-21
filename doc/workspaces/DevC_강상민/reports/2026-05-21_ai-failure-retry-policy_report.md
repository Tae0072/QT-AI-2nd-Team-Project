# Report — 2026-05-21 ai-failure-retry-policy

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-21_ai-failure-retry-policy.md` |
| 관련 F-ID | F-02, F-14, F-15 |
| 기준 문서 | `03_아키텍처_정의서.md`, `04_API_명세서.md`, `05_시퀀스_다이어그램.md`, `07_요구사항_정의서.md`, `18_코드_품질_게이트.md`, `23_도메인_용어사전.md`, `25_기능_명세서.md`, `강상민_공식일정표.md`, `qtai-server/02_ERD_문서.md` |

## 작업 결과

2026-05-21의 "실패·재처리 기준 정리"는 W2 구현 전 정책 고정 작업으로 실행했다. 실제 배치, 관리자 재생성 API, DB migration, Service 구현은 제외하고, 실패 로그와 재처리 판단에 필요한 기준을 기준 문서와 현재 AI 로그 모델에 맞춰 정리했다.

workflow에는 핸들러 실패 로그 필수 필드, 중복 실행 방지 논리 키, 관리자 재생성 가능/불가능 상태를 보강했다. 검토 결과, `inputHash`는 용어사전에 중복 실행 방지 키로 정의되어 있으나 현재 ERD와 구현 코드에는 저장 위치가 아직 없으므로 W2 구현에서 컬럼 또는 내부 로그 저장 위치를 결정해야 한다.

## 확인한 기준

| 구분 | 확인 결과 |
| --- | --- |
| DevC 일정 | 5/21은 실패·재처리 기준 정리, W2는 실제 생성 골격과 관리자 트리거 구현이다. |
| 기능 명세 | F-02는 실패를 로그와 재처리 가능 상태로 남기고, F-14는 검증 실패 산출물 사용자 노출 금지를 요구한다. |
| 요구사항 | AI 생성 실패는 검토 대기 또는 생성 실패 상태로 기록하고, 반복 실패는 관리자 화면에서 확인 가능해야 한다. |
| 품질 게이트 | 이벤트 실패 대응은 `eventId`, event type, handler name, error message 기록과 DB 상태 기준 재시도를 요구한다. |
| 용어사전 | `inputHash`는 중복 실행 방지를 위한 입력 해시이며, 재처리 가능은 DB 상태와 로그 기준으로 다시 처리 가능한 상태다. |
| ERD | `ai_generation_jobs`는 `status`, `target_type`, `target_id`, `prompt_version_id`, `error_message`를 가진다. |
| ERD | `ai_generated_assets.status`는 `VALIDATING`, `APPROVED`, `REJECTED`, `HIDDEN`이며 `REJECTED`는 terminal 상태다. |
| API 명세 | `POST /api/v1/admin/ai/assets/{assetId}/regenerate`는 관리자 재생성 요청 경로이며 감사 로그 대상이다. |
| 시퀀스 | 자동 검증 실패 시 asset은 `REJECTED`, generation job은 `FAILED`로 갱신된다. |

## 정책 정리

| 영역 | 결정 |
| --- | --- |
| 실패 로그 | 기본 필드는 `eventId`, `eventType`, `handlerName`, `errorMessage`다. AI 생성 실패는 `jobId`, target, asset, prompt 정보를 함께 남긴다. |
| 재처리 후보 | `FAILED` job, `REJECTED` asset/log, `NEEDS_REVIEW` log 중 target과 원인 정보가 남은 건이다. |
| 중복 실행 방지 | `targetType + targetId + assetType + promptVersion + inputHash` 논리 키로 판단한다. |
| 차단 상태 | 동일 논리 키의 `QUEUED` 또는 `RUNNING` 작업이 있으면 새 작업을 만들지 않는다. |
| 관리자 재생성 | 기존 job/asset/log를 수정하지 않고 새 generation job과 새 asset을 만든다. |
| 기존 노출본 | 새 산출물이 승인되기 전까지 기존 `APPROVED`/`ACTIVE` 노출본을 유지한다. |
| 로그 금지 항목 | provider raw response, 검증 참조 원문, prompt 원문, secret, token, password, private key를 저장하지 않는다. |

## W2 구현 체크리스트

- `inputHash` 저장 위치를 `ai_generation_jobs` 또는 별도 내부 로그/메타데이터로 확정한다.
- `targetType`, `targetId`, `assetType`, `promptVersion`, `inputHash` 기준 조회 인덱스 또는 중복 차단 쿼리를 설계한다.
- `QUEUED`/`RUNNING` 중복 차단은 batch와 관리자 재생성 경로 모두에 적용한다.
- `FAILED`, `REJECTED`, `NEEDS_REVIEW` 재처리는 새 job/asset/log를 만들고 기존 이력을 보존한다.
- 관리자 재생성, 승인, 반려, 숨김은 `audit_logs` 대상 작업으로 기록한다.
- 실패 사유는 운영 로그용 메시지와 사용자 노출 안내를 분리한다.
- `payloadJson`, `checklistJson`, error message에 원문성 자료와 provider raw response가 들어가지 않는 테스트를 유지한다.

## 수용 기준 점검

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| 5/21 작업이 W2 구현 전 실패·재처리 기준 명세 작업임이 명확하다 | 충족 | 일정표와 workflow 목표에서 W2 사전 명세로 분리했다. |
| 핸들러 실패 로그 필수 필드가 정리된다 | 충족 | workflow에 필드 표를 추가했다. |
| `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs`의 실패·검토·반려 상태 의미가 정리된다 | 충족 | ERD, API, 시퀀스의 상태 전이와 workflow 정책 표를 대조했다. |
| 중복 실행 방지 기준이 target, asset, prompt, input 단위로 정리된다 | 충족 | 논리 키를 정리했고, `inputHash` 저장 위치 gap을 후속 구현 항목으로 분리했다. |
| 관리자 재생성 가능 상태와 불가능 상태가 구분된다 | 충족 | workflow에 상태별 재생성 표를 추가했다. |
| 관리자 재생성 시 기존 이력을 보존하고 새 이력을 추가하는 기준이 정리된다 | 충족 | 새 generation job과 새 asset 생성 기준으로 정리했다. |
| 검증 참조 원문, provider raw response, prompt 원문을 로그에 남기지 않는 기준이 유지된다 | 충족 | workflow와 W2 체크리스트에 금지 항목을 명시했다. |
| W2 구현 시 추가할 테스트 기준이 구체화된다 | 충족 | workflow 테스트 보강 목록과 본 리포트의 W2 체크리스트에 반영했다. |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `rg -n "실패|재처리|중복 실행|관리자 재생성|ai_generation_jobs|ai_generated_assets|ai_validation_logs" doc qtai-server` | 관련 기준 문서, ERD, 기존 AI 로그 모델 문서와 코드 확인 |
| `Select-String ... 강상민_공식일정표.md` | 5/21 기준 정리 작업과 W2 구현 범위 분리 확인 |
| `Select-String ... 18_코드_품질_게이트.md` | 이벤트 실패 로그 필드와 재처리 가능 기준 확인 |
| `Select-String ... 23_도메인_용어사전.md` | `inputHash`, 핸들러 실패 로그, 재처리 가능 용어 확인 |
| `Select-String ... 04_API_명세서.md` | 관리자 AI 산출물 재생성 API와 감사 로그 대상 확인 |
| `Select-String ... 05_시퀀스_다이어그램.md` | 생성 실패와 관리자 승인·재생성 흐름 확인 |
| `Select-String ... 07_요구사항_정의서.md` | 생성 실패, 반려 사유, 반복 실패 관리자 확인 기준 확인 |
| `Select-String ... qtai-server/02_ERD_문서.md` | AI job/asset/log 상태와 필드 확인 |
| `rg -n "inputHash|input_hash|promptVersion|prompt_version_id|requested_by_admin_id|error_message" doc qtai-server` | `inputHash`는 용어 정의만 있고 현재 ERD/코드 저장 위치가 없음을 확인 |

Gradle 테스트는 실행하지 않았다. 이번 workflow의 범위는 문서 기준 정리와 실행 리포트 작성이며, 명세서도 코드 변경을 제외하고 있다.

## 남은 결정 사항

- W2 구현에서 `inputHash`를 어느 테이블 또는 메타데이터에 저장할지 확정해야 한다.
- ERD의 `prompt_version_id` 기준과 현재 구현의 `promptVersion` 문자열 기준을 W2에서 정합화해야 한다.
- ERD의 `CANCELLED` job 상태를 이번 실패·재처리 기준에 포함할지, 별도 취소 정책으로 분리할지 결정해야 한다.
- 중복 실행 방지를 DB unique 제약으로 둘지, service-level 조회와 lock으로 처리할지 결정해야 한다.
- 관리자 재생성 API가 `VALIDATING` 산출물에 대해 어떤 오류 코드를 반환할지 API 계약을 구체화해야 한다.
- 반복 실패 기준을 횟수, 기간, 동일 target 기준 중 무엇으로 집계할지 관리자 화면 설계 시 확정해야 한다.
