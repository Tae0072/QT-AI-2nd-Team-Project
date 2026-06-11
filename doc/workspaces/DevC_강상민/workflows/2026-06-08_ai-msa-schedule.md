# AI MSA 일정표

> 작성자: DevC 강상민
> 기준일: 2026-06-08
> 목적: 팀원들이 AI MSA 분리 작업의 현재 위치, 다음 작업, 상대 의존성을 바로 확인할 수 있게 정리한다.

## 1. 한 줄 요약

AI 쪽은 현재 **계약 고정 → HTTP adapter 기반 → runtime toggle 검증**까지 진행했다.
다음은 provider endpoint가 열리기 전까지 **체크리스트와 fixture를 준비**하고, endpoint가 열리면 **실제 연결 smoke test**로 넘어간다.

## 2. 현재 진행 상태

| 상태 | 작업 | 브랜치 | 결과 |
| --- | --- | --- | --- |
| 완료 | AI 서비스 경계 계약 정리 | `chore/ai-service-boundary-contract` | AI 소유 DB, 외부 의존, 금지 패턴 정리 |
| 완료 | AI client 계약 후속 정리 | `chore/ai-client-contract-followup` | client 메서드명, mock 부정 입력 계약 정리 |
| 완료 | provider system endpoint 계약 동기화 | `chore/ai-system-endpoint-contract-sync` | AI가 호출할 `/api/v1/system/**` 6종 계약 고정 |
| 완료 | HTTP client adapter 기반 추가 | `chore/ai-http-client-adapter-foundation` | mock 기본 유지, `mode=http` 시 HTTP adapter 사용 가능 |
| 진행/PR | runtime toggle 검증 | `test/ai-http-client-runtime-toggle` | Spring context에서 mock/http 전환 조건 검증 |
| 대기 | provider endpoint 실제 개설 | provider 담당 브랜치 | 아직 실제 연결 테스트 불가 |

## 3. 내 작업 일정

| 순서 | 시점 | 내가 할 일 | 브랜치 예시 | 산출물 | 상대 의존 |
| --- | --- | --- | --- | --- | --- |
| 1 | 지금 | runtime toggle PR 정리 및 merge 요청 | `test/ai-http-client-runtime-toggle` | PR, 검증 결과 | 없음 |
| 2 | runtime toggle merge 직후 | provider endpoint readiness checklist 작성 | `docs/ai-provider-endpoint-readiness` | endpoint별 요청/응답/header 체크리스트 | provider 담당자 검토 |
| 3 | checklist 공유 후 | HTTP contract fixture 정리 | `test/ai-http-client-contract-fixtures` | success/error envelope JSON fixture, 테스트 기준 | endpoint 계약 변경 여부 |
| 4 | provider endpoint 열리기 전 | 실제 연결 smoke test 골격 준비 | `test/ai-http-client-provider-smoke-skeleton` | profile-gated smoke test, 실행 방법 | 실제 base-url은 아직 필요 없음 |
| 5 | provider endpoint 열린 날 | 실제 provider 연결 smoke test | `test/ai-http-client-provider-integration` | 6개 endpoint success/error 연결 결과 | provider endpoint, service-token |
| 6 | smoke test 통과 후 | `ai-service` skeleton 생성 계획 확정 | `docs/ai-service-extraction-plan` | ai-service 물리 분리 작업 명세 | Lead/DevOps 결정 |
| 7 | skeleton 합의 후 | AI 코드 물리 이동 | `refactor/ai-service-extraction` | `domain.ai.*`, DeepSeek client 이동 | DB/배포 기준 |
| 8 | 코드 이동 후 | AI DB 소유권 분리 | `chore/ai-db-ownership-split` | AI 전용 DB 연결, migration 분리 | DB migration 순서 |
| 9 | DB 분리 후 | gateway/service-token/JWKS 연결 | `chore/ai-service-routing-auth` | gateway route, 서비스 인증, trace 전파 | Lead/DevOps |
| 10 | 운영 smoke 후 | monolith AI internal 제거 | `refactor/remove-monolith-ai-internals` | `qtai-server` 잔여 AI internal 정리 | 운영 smoke 통과 |

## 4. 이번 주 우선순위

| 우선순위 | 작업 | 완료 기준 | 공유 대상 |
| --- | --- | --- | --- |
| 1 | runtime toggle PR merge | `mode=mock`, `mode=http`, `prod` profile 테스트 통과 | Lead, AI 리뷰어 |
| 2 | provider readiness checklist 작성 | provider 담당자가 구현해야 할 path/query/body/header를 한 문서로 확인 가능 | QT/Bible/Study/Admin 담당자 |
| 3 | contract fixture 정리 | provider가 같은 JSON으로 success/error 응답을 맞출 수 있음 | provider 담당자 |
| 4 | smoke test 골격 준비 | endpoint가 열리면 base-url과 token만 넣고 실행 가능 | Lead, DevOps |

## 5. 상대에게 필요한 것

| 담당 영역 | 필요한 endpoint | 내가 기다리는 것 |
| --- | --- | --- |
| QT/today-qt | `GET /api/v1/system/qt/passages/{passageId}/context` | `QtContextResult` 응답 |
| QT/today-qt | `GET /api/v1/system/qt/passages/today/status?date=YYYY-MM-DD` | `qtDate`, `exists`, `passageId`, `cacheStatus` 응답 |
| Bible | `GET /api/v1/system/bible/verses/{verseId}` | 단건 verse 응답 |
| Bible | `POST /api/v1/system/bible/verses:batch` | batch verse 응답 |
| Bible | `GET /api/v1/system/bible/verses?book=&chapter=&startVerse=&endVerse=` | 범위 verse 응답 |
| Study | `POST /api/v1/system/study/verse-explanations:publish` | 승인 해설 publish 처리 |
| Study | `POST /api/v1/system/study/verse-explanations:hide` | 승인 해설 hide 처리 |
| Audit | `POST /api/v1/system/audit/logs` | 감사 로그 기록 |
| Admin/Auth | `GET /api/v1/system/admin/auth/active` | 활성 관리자 확인 |
| Admin/Auth | `GET /api/v1/system/admin/auth/verify` | 단일 role 검증 |
| Admin/Auth | `GET /api/v1/system/admin/auth/verify-any` | 복수 role 중 하나 검증 |

## 6. endpoint 열리기 전 내 할 일

provider endpoint가 늦게 열리는 동안 나는 아래 작업을 먼저 끝낸다.

| 작업 | 이유 | 결과물 |
| --- | --- | --- |
| readiness checklist | provider 담당자가 구현 전에 요청 계약을 바로 볼 수 있어야 함 | `docs/ai-provider-endpoint-readiness` 문서 |
| contract fixture | 서로 다른 JSON을 기준으로 구현하는 일을 막아야 함 | request/response fixture |
| smoke test skeleton | endpoint가 열리는 날 바로 연결 확인해야 함 | profile-gated test |
| env sample 정리 | base-url/token 설정 실수를 줄여야 함 | `qtai.ai.client.*` 설정 예시 |

후속 readiness checklist와 contract fixture에는 오늘 QT `STALE_FALLBACK` 응답과 F-15 차단 응답의 `blocked_reason` 케이스를 포함한다.

## 7. endpoint 열린 후 내 할 일

endpoint가 열리면 아래 순서로 확인한다.

| 순서 | 확인 항목 | 통과 기준 |
| --- | --- | --- |
| 1 | base-url 연결 | 각 provider 서비스 주소로 요청이 나감 |
| 2 | service-token header | `Authorization: Bearer {service-token}` 전달 |
| 3 | trace header | `traceparent` 전달 가능 |
| 4 | write idempotency | publish/hide/audit에 `Idempotency-Key` 전달 |
| 5 | success envelope | `ApiResponse<T>` 성공 응답 parsing |
| 6 | error envelope | `ApiResponse.error(code,message)`가 `AiClientException`으로 변환 |
| 7 | timeout/failure | provider 장애가 AI 쪽 표준 예외로 변환 |

## 8. ai-service 분리 단계

| 단계 | 내가 할 일 | 완료 기준 |
| --- | --- | --- |
| 서비스 골격 | `ai-service` 실행 단위 생성 | 독립 실행과 health check 가능 |
| 코드 이동 | AI 도메인 내부 구현과 DeepSeek client 이동 | AI 테스트가 새 서비스에서 통과 |
| DB 분리 | AI 소유 테이블만 직접 접근 | QT/Bible/Study/Admin/Audit DB 직접 접근 없음 |
| inbound API | AI system/admin API를 `ai-service`로 승격 | OpenAPI와 Controller 정합 |
| outbound 호출 | provider 호출은 HTTP adapter 사용 | 실제 provider smoke test 통과 |
| gateway 연결 | AI 경로만 `ai-service`로 routing | 기존 외부 API path 유지 |
| monolith 정리 | `qtai-server`의 AI internal 제거 | 회귀 테스트 통과 |

## 9. 완료 기준

AI MSA 1차 분리는 아래 조건을 모두 만족하면 완료로 본다.

- `ai-service`가 독립 실행된다.
- AI 소유 DB만 `ai-service`가 직접 접근한다.
- QT, Bible, Study, Audit, Admin/Auth는 HTTP 계약으로만 호출한다.
- 기존 사용자/관리자 API path는 유지된다.
- gateway가 AI 경로를 `ai-service`로 전달한다.
- `qtai-server`에서 AI internal 구현을 제거해도 테스트가 통과한다.
- success/error envelope, idempotency, trace, service-token 검증이 통과한다.

## 10. 하지 않는 것

현재 내 일정에는 아래 작업을 포함하지 않는다.

- provider endpoint 직접 구현
- Flutter/Admin Web이 바로 `ai-service`를 직접 호출하도록 변경
- provider DB 직접 조회
- 다른 도메인 Entity, Repository, internal Service import
- 실제 service-token/JWKS 발급 구현
- gateway, Docker, Kubernetes, Helm 운영 설정
- AI 자유 챗봇, multi-turn chat, SSE, `/ai/sessions/**`
- RAG, ChromaDB, vector DB, Elasticsearch

## 11. 팀 공유용 현재 결론

내 작업은 지금 **AI 쪽 전환 준비**에 집중되어 있다.
상대 endpoint가 열리기 전까지는 checklist, fixture, smoke test skeleton을 준비한다.
상대 endpoint가 열리면 바로 `mode=http` 실제 연결 검증으로 넘어가고, 그 다음에 `ai-service` 물리 분리를 시작한다.
