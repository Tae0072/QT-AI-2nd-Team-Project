# AI 구현 순서 스케줄표 - 2026-05-28

| 항목 | 내용 |
| --- | --- |
| 목적 | AI 연결 작업을 한 번에 구현하지 않고, 앞으로 어떤 순서로 진행할지 확인하기 위한 일정표 |
| 기준 범위 | 현재는 AI 생성/검증 기반 연결 중심. Q&A(F-15)는 당장 구현 범위에서 제외 |
| 관련 F-ID | F-02, F-14 중심. F-15는 후순위 |
| 기준 문서 | `07_요구사항_정의서.md`, `03_아키텍처_정의서.md`, `04_API_명세서.md`, `25_기능_명세서.md` |
| 주요 코드 경로 | `qtai-server/src/main/java/com/qtai/external/llm/**`, `qtai-server/src/main/java/com/qtai/domain/ai/**` |

## 현재 판단

지금 바로 개발할 AI 범위는 **Q&A가 아니라 DeepSeek 연결과 AI 생성/검증 기반 작업**이다.

F-15 Q&A는 문서상 존재하지만, 아직 `AiController`가 placeholder이고 `ai_qa_requests` 흐름도 구현되어 있지 않다. 따라서 지금 스케줄에서는 Q&A를 "나중에 할 일"로 분리하고, 먼저 F-02/F-14 쪽 기반을 완성한다.

## 전체 스케줄

| 순서 | 작업명 | 목표 | 예상 PR | 우선순위 | 상태 |
| --- | --- | --- | --- | --- | --- |
| 1 | DeepSeek 클라이언트 연결 | 서버에서 실제 DeepSeek API 호출 가능하게 만들기 | PR 1 | 최우선 | 예정 |
| 2 | AI 호출 설정 정리 | API key, model, timeout, base URL을 환경변수 기반으로 정리 | PR 1 | 최우선 | 예정 |
| 3 | AI 호출 테스트 | 실제 API 호출 없이 mock 기반 성공/실패 테스트 작성 | PR 1 | 최우선 | 예정 |
| 4 | 생성 job 처리 흐름 점검 | 기존 `ai_generation_jobs` 생성/상태 전이가 문서와 맞는지 확인 | PR 2 | 높음 | 예정 |
| 5 | 산출물 저장 흐름 점검 | `ai_generated_assets` 저장 흐름과 `QA_RESPONSE` 제외 범위 확인 | PR 2 | 높음 | 예정 |
| 6 | 검증 로그 흐름 점검 | `ai_validation_logs`가 자동 검증 결과를 남길 수 있는지 확인 | PR 2 | 높음 | 예정 |
| 7 | DeepSeek 호출과 generation job 연결 | job 실행 시 LLM 호출 후 산출물 저장까지 이어지게 만들기 | PR 3 | 높음 | 예정 |
| 8 | 자동 검증 최소 구현 | 형식 검증/금지 데이터 검증 등 최소 검증 레이어 추가 | PR 4 | 중간 | 예정 |
| 9 | 실패/재시도 정책 정리 | timeout, 429, 5xx, 검증 실패 시 job 상태 처리 정리 | PR 5 | 중간 | 예정 |
| 10 | 배치 또는 worker 실행 방식 결정 | 04:00 KST 배치, 수동 트리거, scheduler 중 실제 실행 방식 확정 | PR 6 | 중간 | 예정 |
| 11 | 관리자 트리거 연동 확인 | 관리자/시스템 API로 생성 job을 만들고 처리되는지 확인 | PR 7 | 낮음 | 예정 |
| 12 | Q&A 개발 여부 재검토 | F-15를 실제 개발할지, MVP 이후로 미룰지 결정 | 별도 PR | 후순위 | 보류 |

## 주차별 진행안

| 구간 | 할 일 | 완료 기준 |
| --- | --- | --- |
| 1차 | DeepSeek 클라이언트 연결 | `DeepSeekLlmClient.complete()`가 mock 테스트에서 응답 content를 반환 |
| 2차 | 설정/예외 처리 안정화 | API key가 환경변수로만 들어가고, 401/429/5xx/timeout 처리가 테스트됨 |
| 3차 | 기존 AI job/asset/log 구조 점검 | `ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs` 흐름이 문서와 충돌하지 않음 |
| 4차 | LLM 호출과 산출물 저장 연결 | generation job 실행 결과가 asset으로 저장됨 |
| 5차 | 자동 검증 최소 구현 | 검증 실패 산출물이 사용자 노출 상태로 가지 않음 |
| 6차 | 배치/worker 방식 결정 | 실제 실행 주체와 재시도 정책이 문서화됨 |
| 이후 | Q&A 재검토 | F-15를 진행할지 별도 workflow로 다시 작성 |

## 지금 하지 않는 것

| 제외 항목 | 이유 |
| --- | --- |
| F-15 Q&A API 구현 | 현재 우선순위가 아니며, `ai_qa_requests` 설계가 아직 필요함 |
| `POST /api/v1/ai/qa-requests` | Q&A 전용 사용자 API라 이번 AI 연결 스케줄에서 제외 |
| 다중 턴 채팅 | 요구사항에서 금지된 범위 |
| SSE/WebSocket 응답 | 요구사항에서 제외된 범위 |
| Flutter AI 화면 구현 | 서버 AI 연결 이후 별도 작업 |
| 관리자 웹 UI 구현 | 백엔드 API 이후 별도 작업 |
| Redis rate limit | 필요성 검토 후 별도 작업 |

## PR 단위 세부 일정

### PR 1. DeepSeek 클라이언트 연결

| 항목 | 내용 |
| --- | --- |
| 목표 | `DeepSeekLlmClient`의 TODO를 실제 HTTP 호출로 교체 |
| 주요 파일 | `external/llm/DeepSeekLlmClient.java`, `LlmCompletionRequest.java`, `LlmCompletionResponse.java` |
| 설정 | `DEEPSEEK_API_KEY`, `DEEPSEEK_BASE_URL`, `DEEPSEEK_MODEL`, timeout |
| 테스트 | 성공 응답, 401, 429, 5xx, timeout |
| 제외 | DB 저장, job 실행, Q&A API |

### PR 2. AI 생성/검증 기존 구조 점검

| 항목 | 내용 |
| --- | --- |
| 목표 | 지금 만들어진 AI job, asset, validation log가 실제 연결 전에 깨지지 않는지 확인 |
| 주요 파일 | `domain/ai/internal/AiService.java`, `AiLogService.java`, 관련 entity/repository |
| 테스트 | job 생성, asset 저장, validation log 저장, 상태 전이 |
| 제외 | DeepSeek 호출 연결, Q&A |

### PR 3. generation job 실행 흐름 연결

| 항목 | 내용 |
| --- | --- |
| 목표 | 생성 job을 처리할 때 DeepSeek를 호출하고 산출물을 저장 |
| 주요 파일 | `domain/ai/internal` 신규 processor/service |
| 입력 | `ai_generation_jobs`의 `QUEUED` 작업 |
| 출력 | `ai_generated_assets`의 `VALIDATING` 산출물 |
| 제외 | 사용자 Q&A 요청, 관리자 UI |

### PR 4. 자동 검증 최소 구현

| 항목 | 내용 |
| --- | --- |
| 목표 | 산출물이 바로 사용자 노출되지 않도록 최소 검증 레이어 추가 |
| 검증 | JSON 형식, 필수 필드, 금지 번역본/금지 본문 저장 방지, 민감정보 저장 방지 |
| 출력 | `ai_validation_logs` 기록 |
| 실패 처리 | asset `REJECTED`, job 실패 또는 검증 실패 상태 기록 |

### PR 5. 실패/재시도 정책 정리

| 항목 | 내용 |
| --- | --- |
| 목표 | 외부 AI 장애와 검증 실패를 운영 가능한 상태로 남김 |
| 케이스 | timeout, 429, 5xx, 파싱 실패, 검증 실패 |
| 결과 | job `FAILED`, errorMessage 저장, raw response 저장 금지 |
| 후속 | 재시도 횟수와 backoff는 필요 시 별도 PR |

### PR 6. 배치/worker 실행 방식 결정

| 항목 | 내용 |
| --- | --- |
| 목표 | AI 생성 작업을 실제로 언제 누가 처리할지 결정 |
| 후보 | 04:00 KST scheduler, 시스템 API 수동 트리거, Spring Batch |
| 추천 | MVP에서는 scheduler 또는 시스템 API 수동 트리거부터 시작 |
| 결정 필요 | 중복 실행 방지, 실패 재처리, 운영 로그 |

### PR 7. 관리자/시스템 트리거 연동 확인

| 항목 | 내용 |
| --- | --- |
| 목표 | 관리자 또는 시스템 경로에서 생성 job을 만들고 처리까지 이어지는지 확인 |
| 관련 경로 | `/api/v1/system/ai/generation-jobs`, `/api/v1/admin/ai/**` |
| 권한 | `SYSTEM_BATCH`, `REVIEWER`, `SUPER_ADMIN` 기준 확인 |
| 제외 | 관리자 웹 화면 |

## Q&A는 언제 다시 보나

| 조건 | 판단 |
| --- | --- |
| DeepSeek client가 안정화됨 | Q&A 검토 가능 |
| generation job 처리 흐름이 안정화됨 | Q&A 검토 가능 |
| 검증 로그가 안정화됨 | Q&A 검토 가능 |
| F-15 우선순위가 다시 올라옴 | 별도 workflow 작성 |

Q&A를 시작하게 되면 이 문서에 이어서 새 문서를 만든다.

권장 파일명:

```text
doc/workspaces/DevC_강상민/workflows/YYYY-MM-DD_ai-qa-implementation-schedule.md
```

## 공통 주의사항

- secret, token, password, private key 예시는 작성하지 않는다.
- DeepSeek API key는 환경변수로만 주입한다.
- provider raw response 전체를 DB, 로그, 테스트 fixture에 저장하지 않는다.
- 개역개정, ESV, NIV seed/test/fixture/response 데이터를 넣지 않는다.
- 성서유니온/두란노 본문 텍스트를 저장하지 않는다.
- 사용자 요청 시 해설/시뮬레이터를 즉시 생성하는 기능은 만들지 않는다.
- 사용자 앱 API와 관리자/시스템 API 경로를 섞지 않는다.

## 최소 검증 명령

작업 단계별로 필요한 것만 실행하되, 서버 변경 PR을 마무리할 때는 아래 명령을 기준으로 확인한다.

```bash
./gradlew -p qtai-server test
./gradlew -p qtai-server build
npx @stoplight/spectral-cli lint qtai-server/apis/api-v1/openapi.yaml --ruleset .spectral.yaml
gitleaks detect --source . --redact --exit-code 1
git diff --check
```

## 다음에 바로 시작할 작업

가장 먼저 할 일은 **PR 1. DeepSeek 클라이언트 연결**이다.

시작 전에 확인할 것:

| 확인 항목 | 내용 |
| --- | --- |
| API 키 | 로컬 환경변수로만 준비 |
| 호출 방식 | Spring `RestTemplate` 또는 `RestClient` 중 기존 코드 관례에 맞춰 선택 |
| 테스트 방식 | 실제 API 호출 금지, mock 기반 테스트 |
| 완료 기준 | `DeepSeekLlmClient.complete()`가 정상 응답과 오류 응답을 모두 처리 |

