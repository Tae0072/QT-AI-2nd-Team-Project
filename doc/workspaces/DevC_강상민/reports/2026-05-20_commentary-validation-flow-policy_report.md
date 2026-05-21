# Report — 2026-05-20 commentary-validation-flow-policy

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 실행 경로 | 직접 실행 |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-20_commentary-validation-flow-policy.md` |
| 관련 F-ID | F-02, F-08, F-14 |
| 기준 문서 | `03_아키텍처_정의서.md`, `04_API_명세서.md`, `05_시퀀스_다이어그램.md`, `07_요구사항_정의서.md`, `18_코드_품질_게이트.md`, `23_도메인_용어사전.md`, `25_기능_명세서.md`, `강상민_공식일정표.md` |

## 작업 결과

2026-05-20의 "해설 검증 흐름 검토"는 구현 결과 검토가 아니라 W2 구현 전 계약 검토 작업으로 확정했다. W2에서 구현할 `commentary_materials` → `ai_generated_assets` → `ai_validation_logs` → `verse_explanations` 흐름의 입력, 노출, 권한, 로그 저장 기준을 기준 문서와 대조했다.

문서 기준은 서로 충돌하지 않는다. `commentary_materials`는 생성/검증 입력으로 사용하고, 사용자 노출은 승인된 `verse_explanations` 중심으로 제한한다. 검증 참조 원문은 사용자와 일반 관리자에게 노출하지 않으며, 출처 표기는 현재 API 명세의 `sourceLabel`을 우선 기준으로 삼는다.

## 확인한 기준

| 구분 | 확인 결과 |
| --- | --- |
| DevC 일정 | 5/20은 "해설 검증 흐름 검토", W2는 실제 생성 골격/로그/관리자 트리거 구현이다. |
| 입력 자료 | `commentary_materials`는 AI 사전 생성 배치에서 조회되는 입력 자료다. |
| 사용자 노출 | `GET /api/v1/qt/{qtPassageId}/study-content`는 `APPROVED` 콘텐츠만 반환한다. |
| 해설 노출 테이블 | `verse_explanations`는 승인 해설 사용자 노출 테이블이다. |
| 출처 표기 | 사용자 노출 출처 필드는 `sourceLabel` / `source_label` 기준이다. |
| 검증 참조 원문 | 사용자와 일반 관리자에게 노출하지 않는다. 자체 제작자와 `SYSTEM_BATCH`의 검증 경로만 접근한다. |
| 로그 저장 | provider raw response, prompt 원문, 검증 참조 원문은 사용자 응답과 일반 로그에 넣지 않는다. |
| 생성 트리거 | 해설 생성은 사용자 요청 경로가 아니라 04:00 KST 배치 또는 관리자 트리거 경로로 제한한다. |

## W2 구현 체크리스트

- `commentary_materials` 조회는 batch/admin 생성 경로에만 둔다.
- 사용자 API에는 생성 트리거를 열지 않는다.
- `ai_generated_assets`와 `verse_explanations`를 분리한다.
- 검증 통과 또는 관리자 승인 전에는 `verse_explanations.APPROVED`로 연결하지 않는다.
- 활성 승인본 연결 시 기존 `activeUniqueKey`를 `NULL` 처리하고 신규 승인본을 `ACTIVE`로 연결한다.
- `payloadJson`, `checklistJson`, error message에 검증 참조 원문이나 provider raw response를 저장하지 않는다.
- `study-content` 응답은 승인 해설과 `sourceLabel` 중심 출처 표기만 포함한다.

## 수용 기준 점검

| 수용 기준 | 상태 | 근거 |
| --- | --- | --- |
| 5/20 작업이 W2 구현 전 계약 검토 작업임이 명확하다 | 충족 | DevC 일정에서 5/20 검토와 W2 구현 범위가 분리되어 있다. |
| `commentary_materials` 입력과 `verse_explanations` 노출 책임 경계가 정리된다 | 충족 | 아키텍처와 시퀀스 문서가 입력 조회와 노출본 연결을 분리한다. |
| 검증 참조 원문 비공개 범위가 사용자와 일반 관리자까지 포함한다 | 충족 | 요구사항/API 문서 모두 사용자와 일반 관리자 노출 금지를 명시한다. |
| 사용자 응답에는 승인 해설과 `sourceLabel` 중심 출처 표기만 포함한다 | 충족 | API 명세의 `study-content` 응답과 필드 매핑 기준을 확인했다. |
| AI 생성/검증 로그에 원문과 provider raw response를 남기지 않는 기준이 정리된다 | 충족 | 기존 DevC workflow와 코드 품질 게이트 기준이 동일한 금지 기준을 둔다. |
| W2 구현 시 작성해야 할 테스트 기준이 구체화된다 | 충족 | workflow의 테스트 보강 목록과 본 리포트의 W2 체크리스트에 반영했다. |
| 출처 메타데이터 상세 범위가 미확정이면 후속 결정 항목으로 남긴다 | 충족 | `sourceLabel` 외 `sourceId`/절 범위/자료 위치는 후속 결정으로 유지한다. |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `rg -n "commentary_materials|verse_explanations|검증 참조|sourceLabel|study-content" doc` | 관련 기준 문서와 DevC workflow/report에서 용어 확인 |
| `Select-String ... 강상민_공식일정표.md` | 5/20 검토 작업과 W2 구현 작업 분리 확인 |
| `Select-String ... 04_API_명세서.md` | `study-content`, `sourceLabel`, 검증 참조 작업 API 권한 확인 |
| `Select-String ... 07_요구사항_정의서.md` | 검증용 한국어 주석 자료의 사용자/일반 관리자 노출 금지 확인 |
| `Select-String ... 03_아키텍처_정의서.md` | `commentary_materials` 입력과 `verse_explanations` 사용자 노출본 연결 흐름 확인 |
| `Select-String ... 05_시퀀스_다이어그램.md` | `APPROVED` + `ACTIVE` 조회와 승인 시 활성본 교체 흐름 확인 |
| `Select-String ... 18_코드_품질_게이트.md` | 검증 참조 자료 사용자 응답 DTO 노출 금지 확인 |

Gradle 테스트는 실행하지 않았다. 이번 workflow는 코드 구현을 제외한 문서 검토와 실행 리포트 작성 범위이며, 명세서의 검증 계획도 Gradle 테스트 제외를 명시한다.

## 남은 결정 사항

- `sourceLabel`만으로 사용자 출처 표기가 충분한지 결정해야 한다.
- 내부 추적용 `sourceId`, 절 범위, 자료 위치 메타데이터가 필요하면 사용자 DTO가 아니라 내부 로그 또는 관리자 검토 전용 필드로 분리해야 한다.
- W2 구현에서 batch/admin 호출부가 생길 때 시간 정책(`Clock` 또는 KST 기준 주입 방식)을 결정해야 한다.
- 관리자 승인 API에서 자동 검증 통과 산출물을 즉시 노출할지, 수동 승인 후 노출할지 운영 정책을 확정해야 한다.
