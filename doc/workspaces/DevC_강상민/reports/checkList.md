# Lead 확인 체크리스트

작성 기준: 2026-05-26 W2 스케줄, DevC report/workflow 기준.

목적: DevC AI 생성/검증 로그 작업에서 구현 전 Lead 또는 공통 소유자 확인이 필요한 항목을 한 곳에서 추적한다.

## 확정 완료

| 우선순위 | 확인 항목 | 확정 내용 | 다음 반영 | 관련 근거 |
| ---: | --- | --- | --- | --- |
| 1 | 관리자 AI 산출물 조회 `OPERATOR` 허용 여부 | `OPERATOR`는 `GET /api/v1/admin/ai/assets` 목록/상세 조회도 차단한다. `ADMIN + REVIEWER/SUPER_ADMIN`만 허용한다. | 현재 구현과 OpenAPI의 `REVIEWER/SUPER_ADMIN` 제한을 유지한다. 권한 매트릭스의 `OPERATOR` 조회 가능 표현은 후속 문서 정합화 대상이다. | `04_API_명세서.md` 2.2 권한 매트릭스, 4.7.3 AI 산출물 검증 |
| 2 | `validation_reference_jobs` API와 `ai_validation_checklist_versions` 관리 API 우선순위 | `ai_validation_checklist_versions` 관리 API를 먼저 구현한다. `validation_reference_jobs` API는 후순위로 둔다. | 다음 AI 관리 API PR은 체크리스트 버전 생성/활성화/폐기/조회 흐름을 우선 반영한다. | W2 스케줄, F-14 |
| 3 | `inputHash` 저장 위치와 중복 방지 키 | `inputHash`는 `ai_generation_jobs.input_hash` 컬럼으로 저장한다. active job 중복 방지 unique key에도 포함한다. | 기준 키는 `job_type + target_type + target_id + prompt_version_id + input_hash + active_unique_key`로 반영한다. | `2026-05-21_ai-failure-retry-policy_report.md` |
| 4 | `SUMMARY`/`GLOSSARY` 독립 generation job type 지원 여부 | `SUMMARY`/`GLOSSARY`는 독립 generation job type으로 열지 않는다. `AiGeneratedAssetType`으로만 유지한다. | 시스템 생성 job API는 `DAILY_QT_EXPLANATION`, `DAILY_QT_SIMULATOR`만 허용하고, `SUMMARY`/`GLOSSARY` 산출물은 필요 시 `DAILY_QT_EXPLANATION` job의 결과로 등록한다. | `2026-05-26_ai-prompt-version-id-mapping_report.md` |

## 공통 API/보안 확인

| 우선순위 | 확인 항목 | 현재 판단 | 결정 필요 내용 | 관련 근거 |
| ---: | --- | --- | --- | --- |
| 5 | `service_accounts` 기반 시스템 토큰 검증 방식 | 현재 `/api/v1/system/ai/**`는 컨트롤러에서 `SYSTEM_BATCH`/`ROLE_SYSTEM_BATCH` authority를 확인하는 최소 방어선 | service account credential 저장 방식, 토큰 포맷, 검증 필터 위치, `/api/v1/system/**` 전역 보안 설정 범위 확정 | `03_아키텍처_정의서.md` 7.3, W2 스케줄 |
| 6 | `service_accounts` migration 반영 여부 | ERD에는 `service_accounts`가 있으나 현재 migration에는 없음 | 어느 migration에서 `service_accounts`를 생성할지, 감사 로그 FK와 함께 갈지 확정 | `qtai-server/02_ERD_문서.md`, 현재 migration 상태 |
| 7 | 감사 로그 UseCase 계약과 AI 작업 연결 | 관리자 재생성, 승인/반려/숨김, 시스템 배치 작업은 감사 로그 대상 | `WriteAuditLogUseCase` 계약, `SYSTEM_BATCH` actor와 service account 연결 정책, `AI_REGENERATE_REQUEST` 기록 시점 확정 | `2026-05-21_admin-ai-generation-trigger-api_report.md`, `04_API_명세서.md` |
| 8 | 공통 `ApiResponse.ErrorBody.fields` 지원 여부 | OpenAPI에는 선반영하지 않음 | validation error field 목록을 공통 응답에 추가할지, 추가한다면 Java 구현과 OpenAPI를 같은 PR에서 정합화할지 확정 | `2026-05-26_ai-system-openapi-contract_report.md` |
| 9 | `.spectral.yaml` ruleset 위치 | Spectral lint가 ruleset 부재로 실패 | 저장소 루트에 ruleset을 추가할지, 별도 표준 위치를 둘지 확정 | `2026-05-26_ai-system-openapi-contract_report.md` |

## AI 운영 정책 확인

| 우선순위 | 확인 항목 | 현재 판단 | 결정 필요 내용 | 관련 근거 |
| ---: | --- | --- | --- | --- |
| 10 | 자동 검증 통과 산출물 노출 정책 | 현재 시스템은 산출물/검증 로그 접수까지만 구현 | 자동 검증 `PASSED` 후 즉시 사용자 노출할지, 관리자 수동 승인 후 노출할지 확정 | `2026-05-20_commentary-validation-flow-policy_report.md` |
| 11 | `APPROVED` 산출물 재생성/교체 정책 | 현재 관리자 재생성은 `REJECTED`/`HIDDEN` 허용, `VALIDATING`/`APPROVED` 차단 | 운영자가 `APPROVED`도 조건부 재생성할 수 있는지, 기존 노출본 교체 방식 확정 | `2026-05-21_admin-ai-generation-trigger-api_report.md` |
| 12 | 사용자 출처 표기 메타데이터 범위 | 사용자 응답은 `sourceLabel` 중심으로 정리됨 | `sourceLabel`만으로 충분한지, 내부 추적용 `sourceId`/절 범위/자료 위치 메타데이터를 별도 저장/노출할지 확정 | `2026-05-20_commentary-validation-flow-policy_report.md` |
| 13 | 반복 실패 집계 기준 | 실패 로그와 재처리 가능 상태는 정리됨 | 반복 실패를 횟수, 기간, 동일 target 기준 중 무엇으로 집계할지 확정 | `2026-05-21_ai-failure-retry-policy_report.md` |
| 14 | `CANCELLED` generation job 상태 사용 여부 | 현재 구현은 핵심 상태 위주로 동작 | ERD의 `CANCELLED`를 이번 실패/재처리 정책에 포함할지, 별도 취소 정책으로 분리할지 확정 | `2026-05-21_ai-failure-retry-policy_report.md` |

## 결정 후 처리 메모

- 결정된 항목은 이 파일에서 상태를 갱신하고, 해당 workflow/report 또는 스케줄 문서에 반영한다.
- API 계약이 바뀌는 항목은 `qtai-server/apis/api-v1/openapi.yaml`과 구현 테스트를 같은 PR에서 맞춘다.
- 보안/감사 항목은 공통 소유자와 충돌하지 않게 별도 PR로 분리한다.
- 2026-05-27 Lead 협의로 우선순위 1~4 항목은 확정 완료했다.
