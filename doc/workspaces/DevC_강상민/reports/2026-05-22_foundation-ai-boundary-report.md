# Report - 2026-05-22 foundation-ai-boundary

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 실행 경로 | 직접 실행 |
| 관련 일정 | `doc/workspaces/DevC_강상민/강상민_공식일정표.md` - 2026-05-22 Foundation 5/5 참여 검증 |
| 관련 F-ID | Foundation, F-14, F-15 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `18_코드_품질_게이트.md`, `qtai-server/02_ERD_문서.md` |

## 결론

2026-05-22 Foundation 5/5 참여 검증 중 DevC 담당 항목인 "AI 경계와 금지 사용자 경로 검증 근거 제출"은 현재 저장소 기준 통과로 판정한다.

AI 도메인은 F-15 사실 기반 Q&A를 단발 요청과 결과 조회 흐름으로만 열어두고 있으며, 자유 챗봇, 다중 턴 세션, SSE 스트리밍, `/ai/sessions/**` 사용자 경로는 구현되어 있지 않다. 현재 `AiController`도 실제 매핑 구현 없이 F-15 Q&A placeholder만 가지고 있고, 주석으로 streaming/free chat/multi-turn 제외 기준을 명시한다.

## 기준 확인

| 기준 | 확인 결과 |
| --- | --- |
| DevC 일정 | 2026-05-22 작업은 AI 경계와 금지 사용자 경로 검증 근거 제출이다. |
| Lead 일정 | Foundation 5/5가 모두 통과해야 W2로 넘어간다. |
| 요구사항 | F-15는 단어, 시대상, 역사에 한정한 사실 기반 단발 Q&A다. AI 자유 챗봇은 제외 대상이다. |
| 아키텍처 | Q&A는 이전 질문 맥락을 유지하지 않는다. AI 산출물과 사용자 노출 콘텐츠를 분리한다. |
| ERD | 다중 턴 채팅 세션 테이블은 MVP 제외 대상이다. Q&A 응답은 `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs` 흐름에 연결한다. |

## 검증 항목

| 항목 | 판정 | 근거 |
| --- | --- | --- |
| `/ai/sessions/**` 사용자 경로 없음 | 통과 | `qtai-server/src`, `qtai-server/apis` 검색 결과 없음 |
| SSE/streaming 구현 없음 | 통과 | `SseEmitter`, `ServerSentEvent`, `Flux<`, session mapping 검색 결과 없음 |
| 자유 챗봇/다중 턴 계약 없음 | 통과 | 실제 endpoint나 OpenAPI 계약 없음. `AiController` 주석은 제외 기준만 명시 |
| F-15 Q&A 단발 흐름 유지 | 통과 | `AiController` placeholder는 `POST /qa-requests`, `GET /qa-requests/{requestId}`만 예정 |
| AI 도메인 금지 import 없음 | 통과 | AI 도메인에서 타 도메인의 `internal`, `web`, `client` 직접 import 검색 결과 없음 |
| 검증 전 산출물 노출 차단 기준 유지 | 통과 | 기존 AI 로그/산출물 테스트가 `PASSED`, `NEEDS_REVIEW`에서 자동 승인하지 않음을 검증 |

## 실행한 검증 명령

| 명령 | 결과 |
| --- | --- |
| `rg -n "/ai/sessions" qtai-server/src qtai-server/apis -S` | 결과 없음. 금지 사용자 경로 없음 |
| `rg -n "\bSSE\b\|SseEmitter\|ServerSentEvent\|Flux<\|@.*Mapping.*sessions\|free chat\|multi-turn\|다중 턴\|자유 챗봇" qtai-server/src/main/java qtai-server/apis -S` | 실제 구현 없음. `AiController`의 제외 기준 주석 1건만 확인 |
| `rg -n "import com\.qtai\.domain\.(member\|bible\|qt\|study\|note\|sharing\|report\|notification\|praise\|mission\|admin\|audit)\.(internal\|web\|client)" qtai-server/src/main/java/com/qtai/domain/ai -S` | 결과 없음. AI 도메인의 타 도메인 내부 구현 직접 import 없음 |
| `Get-Content -Raw qtai-server/src/main/java/com/qtai/domain/ai/web/AiController.java` | F-15 단발 Q&A placeholder와 streaming/free chat/multi-turn 제외 주석 확인 |
| `Get-ChildItem -Recurse -File qtai-server/src/main/java/com/qtai/domain/ai` | AI 도메인 파일 범위 확인 |

## 확인된 코드 근거

| 파일 | 확인 내용 |
| --- | --- |
| `qtai-server/src/main/java/com/qtai/domain/ai/web/AiController.java` | base path는 `/api/v1/ai` placeholder이며, 예정 경로는 Q&A 요청과 결과 조회뿐이다. |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/RequestAiQaUseCase.java` | F-15 사용자 요청 진입 계약이 Q&A 요청 단위로 분리되어 있다. |
| `qtai-server/src/main/java/com/qtai/domain/ai/api/GetAiQaResultUseCase.java` | Q&A 결과 조회 계약이 별도 UseCase로 분리되어 있다. |
| `qtai-server/src/main/java/com/qtai/domain/ai/client/qt/*` | AI 도메인이 QT 컨텍스트를 client adapter 경계로 조회하는 구조다. |
| `qtai-server/src/main/java/com/qtai/external/llm/*` | 외부 LLM 호출은 공통 external 포트로 분리되어 있다. |

## W2 유지 조건

- 사용자 경로로 `/api/v1/ai/sessions/**`를 만들지 않는다.
- `SseEmitter`, `ServerSentEvent`, `Flux` 기반 AI 스트리밍 endpoint를 만들지 않는다.
- Q&A 요청은 단발성으로 처리하고 이전 질문 맥락을 저장하거나 이어 붙이지 않는다.
- AI 응답은 검증 로그를 거친 뒤 사용자 노출 상태로 확정한다.
- AI 도메인은 타 도메인의 `api` 계약 또는 자기 도메인의 `client` adapter만 사용하고, 타 도메인 `internal`, `web`, `client` 타입을 직접 import하지 않는다.

## 검증 범위 제한

이번 작업은 Foundation 5/5 참여 검증용 report 작성이다. 코드 변경이 없으므로 Gradle build/test, Spectral, gitleaks는 실행하지 않았다. W2에서 실제 endpoint, service, migration이 추가되면 해당 PR 범위에서 전체 품질 게이트를 다시 실행해야 한다.
