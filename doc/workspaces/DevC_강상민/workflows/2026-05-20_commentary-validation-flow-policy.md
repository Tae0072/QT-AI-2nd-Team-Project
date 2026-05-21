# Workflow — 2026-05-20 commentary-validation-flow-policy

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-pre-generation-validation` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-08, F-14 |
| 트리거 | 2026-05-20 DevC 일정의 "해설 검증 흐름 검토" 작업을 W2 구현 전 계약 검토로 구체화 |
| 기준 문서 | 아래 기준 문서 목록 참고 |
| 해당 경로 | 아래 해당 경로 목록 참고 |

## 기준 문서

- `doc/workspaces/DevC_강상민/강상민_공식일정표.md`
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
- `qtai-server/src/main/java/com/qtai/domain/study/**`

## 작업 목표

이번 작업은 W2의 실제 구현을 시작하기 전에 `commentary_materials` 기반 해설 생성/검증 흐름의 계약을 문서 기준으로 확정하는 사전 검토 작업이다. 구현 대상 코드를 작성하기보다, 검증 참조 원문을 어디에도 잘못 노출하지 않으면서 승인된 `verse_explanations`와 출처 메타데이터만 사용자에게 제공하는 기준을 정리한다.

검토 결과는 W2의 `commentary_materials` → `verse_explanations` 생성 골격, AI 생성/검증 로그, 관리자 트리거 초안 작업에서 바로 사용할 수 있는 체크리스트가 되어야 한다. 특히 사용자 요청 경로에서 해설이 생성되지 않고, 배치 또는 관리자 트리거 경로에서만 생성되도록 경계를 고정한다.

## 범위

- `commentary_materials`와 `commentary_material_verses`를 해설 생성/검증 입력 자료로 해석한다.
- `verse_explanations`를 사용자에게 노출 가능한 승인 해설 테이블로 해석한다.
- 검증 참조 원문은 사용자와 일반 관리자에게 노출하지 않는 비공개 자료로 확정한다.
- 사용자 응답에는 검증 참조 원문, prompt 원문, provider raw response를 포함하지 않는 기준을 명시한다.
- 사용자 노출 출처는 현재 API 명세의 `sourceLabel` 기준으로 우선 정리한다.
- 내부 추적용 출처 식별자, 절 범위, 자료 위치가 필요한 경우 사용자 DTO가 아니라 내부 로그 또는 관리자 검토 전용 메타데이터로 분리할지 후속 결정 항목으로 남긴다.
- W2 구현자가 확인할 데이터 흐름, 권한 경계, 로그 저장 금지 항목, 승인 노출 기준을 체크리스트화한다.

## 제외 범위

- 실제 DeepSeek 또는 외부 LLM 호출 구현은 하지 않는다.
- 배치 스케줄러, 관리자 재생성 API, 관리자 승인 API 구현은 하지 않는다.
- DB migration, Entity, Repository, Service 코드는 이번 작업에서 수정하지 않는다.
- `GET /api/v1/qt/{qtPassageId}/study-content` 실제 응답 구현은 하지 않는다.
- 프론트 화면, 관리자 콘솔 화면, 운영 대시보드는 이번 작업에서 구현하지 않는다.
- 검증용 한국어 주석 원문을 조회하거나 샘플 데이터로 문서에 붙여 넣지 않는다.

## 검토 기준

| 기준             | 결정                                                                                                        |
| ---------------- | ----------------------------------------------------------------------------------------------------------- |
| 입력 자료        | `commentary_materials`는 AI 해설 생성/검증 입력으로만 사용한다.                                             |
| 사용자 노출 결과 | 승인된 `verse_explanations`만 `study-content` 응답으로 노출한다.                                            |
| 검증 참조 원문   | 사용자 화면, 일반 관리자 화면, 사용자 API 응답, 에러 메시지, 일반 로그에 노출하지 않는다.                   |
| 접근 가능 주체   | 자체 제작자와 `SYSTEM_BATCH`의 필요한 검증 경로로 제한한다.                                                 |
| 출처 표기        | 사용자 응답에는 원문이 아닌 `sourceLabel` 같은 최소 출처 표기만 포함한다.                                   |
| 로그 저장        | `payloadJson`, `checklistJson`, error message에는 provider raw response나 검증 참조 원문을 저장하지 않는다. |
| 생성 트리거      | 해설·시뮬레이터 생성은 04:00 KST 배치 또는 관리자 트리거에서만 가능하다.                                    |

## 구현 순서

1. DevC 공식 일정표의 2026-05-20 작업과 W2 작업의 관계를 확인한다.
2. 요구사항 정의서에서 검증용 한국어 주석 자료의 접근 권한을 확인한다.
3. API 명세에서 사용자 노출 API인 `GET /api/v1/qt/{qtPassageId}/study-content`의 응답 필드와 `sourceLabel` 매핑을 확인한다.
4. 시퀀스 다이어그램에서 AI 사전 생성 배치가 `commentary_materials`를 조회하고 검증 통과 후 `verse_explanations`에 연결하는 흐름을 확인한다.
5. 아키텍처 문서에서 `ai_generated_assets`와 사용자 노출 테이블(`verse_explanations`, `simulator_clips`) 분리 원칙을 확인한다.
6. 기존 DevC workflow와 report에서 이미 정리된 AI UseCase 계약, AI 생성/검증 로그 모델, raw response 저장 금지 기준을 확인한다.
7. W2 구현 체크리스트를 작성하고, 애매한 출처 메타데이터 범위를 후속 결정 항목으로 남긴다.

## 테스트 보강 목록

| 테스트 파일                                          | 추가할 검증                                                                                                             |
| ---------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------- |
| `qtai-server/src/test/java/com/qtai/domain/study/**` | `study-content` 응답이 `APPROVED` + `ACTIVE` `verse_explanations`만 반환하고 검증 참조 원문 필드를 포함하지 않는지 검증 |
| `qtai-server/src/test/java/com/qtai/domain/ai/**`    | `payloadJson`과 `checklistJson`에 provider raw response 또는 검증 참조 원문이 저장되지 않는지 검증                      |
| `qtai-server/src/test/java/com/qtai/domain/ai/**`    | 사용자 요청 경로가 해설 생성 UseCase를 직접 호출하지 못하고 batch/admin 경로만 생성 트리거를 사용할 수 있는지 검증      |
| `qtai-server/src/test/java/com/qtai/domain/ai/**`    | 검증 실패 또는 `NEEDS_REVIEW` 상태의 산출물이 `verse_explanations.APPROVED`로 연결되지 않는지 검증                      |

## 수용 기준

- [ ] 2026-05-20 작업이 W2 구현 전 계약 검토 작업임이 문서에 명확히 남는다.
- [ ] `commentary_materials` 입력과 `verse_explanations` 사용자 노출 결과의 책임 경계가 정리된다.
- [ ] 검증 참조 원문 비공개 범위가 사용자와 일반 관리자까지 포함한다.
- [ ] 사용자 응답에는 승인 해설과 `sourceLabel` 중심 출처 표기만 포함한다는 기준이 정리된다.
- [ ] AI 생성/검증 로그에 원문과 provider raw response를 남기지 않는 기준이 정리된다.
- [ ] W2 구현 시 작성해야 할 테스트 기준이 구체화된다.
- [ ] 출처 메타데이터의 상세 범위가 미확정이면 후속 결정 항목으로 남긴다.

## Subagent Decision

### 권장 여부

Subagent 사용은 권장하지 않는다.

### 판단 근거

- 이번 작업은 코드 구현보다 SSoT 문서 사이의 정책 해석과 계약 정리가 핵심이다.
- 편집 대상이 단일 workflow 문서에 가깝고, 병렬 편집 시 용어와 정책 판단이 어긋날 가능성이 크다.
- W2 구현 전 결정 항목을 남기는 작업이므로 메인 에이전트가 직접 문맥을 유지하며 작성하는 편이 안전하다.

### 위임 가능한 작업

| Worker    | 위임 작업           | 편집 가능 경로 |
| --------- | ------------------- | -------------- |
| 해당 없음 | 병렬 위임 대상 없음 | 해당 없음      |

### 직접 실행 판단

메인 에이전트가 문서 확인, 정책 해석, workflow 작성, 최종 검증을 직접 수행한다.

## 검증 계획

- `rg -n "commentary_materials|verse_explanations|검증 참조|sourceLabel|study-content" doc`로 기준 문서의 용어를 재확인한다.
- `doc/workspaces/DevC_강상민/강상민_공식일정표.md`의 5/20 작업과 W2 작업이 충돌하지 않는지 확인한다.
- `doc/프로젝트 문서/04_API_명세서.md`에서 사용자 노출 응답 필드가 원문이 아닌 승인 해설과 출처 표기 중심인지 확인한다.
- `doc/프로젝트 문서/07_요구사항_정의서.md`에서 검증용 한국어 주석 자료 접근 권한이 사용자/일반 관리자에게 닫혀 있는지 확인한다.
- 이번 workflow 작성 자체는 코드 변경이 아니므로 Gradle 테스트는 실행하지 않는다.

## 후속 작업으로 넘길 항목

- W2 `commentary_materials` → `verse_explanations` 생성 골격 구현
- AI 생성/검증 로그와 실제 생성 UseCase 연결
- 관리자 트리거와 `SYSTEM_BATCH` 실행 주체 기록 방식 구현
- `sourceLabel`만으로 출처 표시가 충분한지, 내부 추적용 `sourceId`/절 범위/자료 위치 메타데이터가 필요한지 결정
- 관리자 승인 시 `verse_explanations.APPROVED` + `activeUniqueKey='ACTIVE'` 연결 정책 구현
- 사용자 응답, 관리자 응답, 로그 저장소에 검증 참조 원문이 섞이지 않는 테스트 보강
