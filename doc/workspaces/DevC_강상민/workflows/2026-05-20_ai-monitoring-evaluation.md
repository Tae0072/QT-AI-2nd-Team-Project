# Workflow — 2026-05-20 ai-monitoring-evaluation

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-monitoring-evaluation` |
| PR 대상 | `dev` |
| 관련 F-ID | F-14, F-15 |
| 기준 문서 | `07_요구사항_정의서.md` §6.14, `04_API_명세서.md` §4.7.8, §4.7.10, `18_코드_품질_게이트.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/**`, `qtai-server/src/test/java/com/qtai/domain/ai/**` |

## 작업 목표

AI 운영자가 검증 실패율, 반려 사유, 재생성 횟수, 차단된 Q&A 유형을 확인하고, 실패 사례를 평가 셋 후보로 등록할 수 있는 운영 기반을 만든다.

## 문제 정의

F-14의 품질 관리는 단일 산출물 승인으로 끝나지 않는다. 반복 실패 유형을 평가 셋과 체크리스트 개선으로 되돌려야 하며, 시연 전 최소 평가 셋 기준을 만족해야 한다.

## API 범위

| API | 권한 | 책임 |
| --- | --- | --- |
| `GET /api/v1/admin/ai/monitoring` | ADMIN/SUPER_ADMIN | 기간별 AI 운영 지표 조회 |
| `GET /api/v1/admin/ai/validation-checklists` | REVIEWER/SUPER_ADMIN | 체크리스트 목록 조회 |
| `POST /api/v1/admin/ai/validation-checklists` | REVIEWER/SUPER_ADMIN | 체크리스트 생성 |
| `POST /api/v1/admin/ai/validation-checklists/{id}/activate` | REVIEWER/SUPER_ADMIN | 체크리스트 활성화 |
| `POST /api/v1/admin/ai/evaluation-sets` | REVIEWER/CONTENT_CREATOR/SUPER_ADMIN | 평가 셋 생성 |
| `POST /api/v1/admin/ai/evaluation-sets/{setId}/cases` | REVIEWER/CONTENT_CREATOR/SUPER_ADMIN | 평가 케이스 후보 등록 |

## 지표 기준

- 생성 작업 성공/실패 수
- asset type별 승인/반려/숨김 수
- checklist version별 검증 결과 분포
- F-15 Q&A `BLOCKED` 사유 분포
- 외부 AI 실패 수
- 재생성 요청 수

## 평가 셋 기준

- 평가 셋은 `QA`, `EXPLANATION`, `SIMULATOR` 등 대상 유형을 구분한다.
- 시연 전 최소 10개 본문을 포함해야 한다.
- 서사, 교훈, 시가, 예언, 복음서, 서신 등 서로 다른 유형이 섞이도록 구성한다.
- 사용자 신고와 운영자 발견 실패 사례는 후보로 등록하고, 확정 여부는 운영자가 판단한다.

## 체크리스트 버전 기준

- 체크리스트가 변경되면 새 버전을 만든다.
- 활성 체크리스트는 타입별 1개만 유지한다.
- 폐기된 체크리스트는 기존 산출물 이력 조회를 위해 삭제하지 않는다.
- 산출물 승인 시 적용된 checklist version을 추적할 수 있어야 한다.

## 제외 범위

- 관리자 프론트엔드 차트 UI 구현은 제외한다.
- AI 모델 품질 자동 튜닝은 제외한다.
- RAG, vector DB, Elasticsearch는 도입하지 않는다.

## 구현 순서

1. 기존 AI 로그 모델에서 monitoring query에 필요한 필드를 확인한다.
2. `AiMonitoringQueryRepository` 또는 동등한 조회 전용 repository를 만든다.
3. 기간 필터와 KST 기준 날짜 처리를 고정한다.
4. 체크리스트 버전 Entity와 활성화 상태 전이를 구현한다.
5. 평가 셋과 평가 케이스 후보 등록 흐름을 구현한다.
6. 사용자 신고 연계는 report 도메인 계약 확정 전까지 target type/id 기반 후보 등록만 제공한다.

## 수용 기준

- [ ] 운영 지표는 기간 필터 기준으로 조회된다.
- [ ] 체크리스트 활성화 시 동일 타입의 기존 활성 버전은 비활성화된다.
- [ ] 산출물 승인에 필요한 checklist version을 조회할 수 있다.
- [ ] 평가 케이스 후보는 source type과 source id를 가진다.
- [ ] Q&A 차단 사유 분포를 확인할 수 있다.
- [ ] 검증 참조 원문과 provider secret은 monitoring 응답에 포함되지 않는다.

## 테스트 계획

- Query 테스트: 기간 필터, asset type별 집계, blocked reason 집계
- Service 테스트: 체크리스트 생성/활성화/폐기 상태 전이
- Controller 테스트: 권한별 접근 제어, 공통 envelope
- 회귀 테스트: 금지 필드 미노출

## 검증 명령

- `./gradlew -p qtai-server test --tests "*AiMonitoring*"`
- `./gradlew -p qtai-server test --tests "*AiValidationChecklist*"`
- `./gradlew -p qtai-server build`
