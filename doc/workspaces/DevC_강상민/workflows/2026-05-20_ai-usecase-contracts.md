# Workflow — 2026-05-20 ai-usecase-contracts

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-usecase-contracts` |
| PR 대상 | `dev` |
| 관련 F-ID | F-02, F-14, F-15 |
| 기준 문서 | `03_아키텍처_정의서.md` §3.1, §8, `04_API_명세서.md`, `CODE_CONVENTION.md` |
| 담당 경로 | `qtai-server/src/main/java/com/qtai/domain/ai/api/**`, `qtai-server/src/main/java/com/qtai/domain/ai/client/**`, `qtai-server/src/main/java/com/qtai/external/llm/**` |

## 작업 목표

AI 도메인의 공개 UseCase와 내부 호출 경계를 고정한다. 다른 도메인은 `domain.ai.api`의 interface와 DTO만 바라보고, AI 도메인은 필요한 본문·QT 컨텍스트를 상대 도메인의 `api` 계약 또는 임시 mock adapter를 통해 받는다.

## 문제 정의

AI 도메인은 QT 본문, 성경 구절, 관리자 권한, 감사 로그와 연결되지만 각 도메인의 Entity나 Repository를 직접 가져오면 모듈 경계가 깨진다. 작업 초기에 UseCase 계약을 정리해야 이후 구현 PR이 서로 다른 방향으로 확장되지 않는다.

## 범위

- `domain.ai.api`에 외부 도메인이 호출할 UseCase interface를 정리한다.
- `api/dto`에는 command/result record만 둔다.
- `domain.ai.client.qt`에는 QT 컨텍스트 조회 mock adapter를 둔다.
- LLM 호출은 `external.llm` client 계약을 통해서만 수행하도록 의존 방향을 정한다.
- 기존 `GenerateAiResponseUseCase`가 자유 챗봇처럼 해석되지 않도록 F-15 단발 Q&A 또는 사전 생성 명칭으로 교정한다.

## 제외 범위

- 실제 관리자 HTTP API 구현은 제외한다.
- 실제 LLM provider 요청/응답 매핑은 제외한다.
- 타 도메인의 Entity, Repository, Service 수정은 제외한다.

## UseCase 초안

| UseCase | 호출 주체 | 책임 |
| --- | --- | --- |
| `RequestAiQaUseCase` | `web` 또는 QT 화면 API | F-15 질문 접수, 차단, 비동기 작업 생성 |
| `GetAiQaResultUseCase` | `web` | 요청자 본인 Q&A 결과 조회 |
| `CreateAiGenerationJobUseCase` | `batch`, `admin` | 해설·시뮬레이터 사전 생성 작업 생성 |
| `RegisterAiGeneratedAssetUseCase` | `batch`, 내부 작업 | AI 산출물 등록 |
| `RegisterAiValidationLogUseCase` | `batch`, 검증기 | 검증 로그 등록 |
| `ReviewAiAssetUseCase` | 관리자 검토 API | 승인, 반려, 숨김, 재생성 요청 |

## DTO 기준

- Request/Response가 아니라 UseCase용 `Command`, `Result` 명칭을 사용한다.
- 외부 HTTP DTO는 `web` 패키지에서 별도로 만든다.
- DTO에는 Entity를 넣지 않는다.
- 사용자 응답에 검증용 주석 원문, prompt 원문, provider raw response를 넣지 않는다.

## 구현 순서

1. 기존 `GenerateAiResponseUseCase`, `AiPromptRequest`, `AiResponse`의 이름과 책임을 확인한다.
2. 자유 챗봇처럼 보이는 계약은 F-15 단발 Q&A 또는 사전 생성 계약으로 분리한다.
3. `api/dto` record를 command/result 중심으로 추가한다.
4. `client/qt/GetQtUseCaseMock.java`의 TODO와 반환 DTO를 실제 후속 통합 기준에 맞춘다.
5. ArchUnit 또는 경계 테스트에서 `domain.ai`가 타 도메인의 `internal`, `web`, `client`를 import하지 않는지 확인한다.

## 수용 기준

- [ ] 다른 도메인이 사용할 AI 기능은 `domain.ai.api`에 interface로 존재한다.
- [ ] HTTP Request/Response DTO와 UseCase DTO가 섞이지 않는다.
- [ ] `GenerateAiResponseUseCase`가 세션형 AI나 자유 챗봇으로 오해되지 않는다.
- [ ] AI 도메인의 타 도메인 의존은 `api` 또는 `client/{domain}` adapter로만 표현된다.
- [ ] 외부 LLM 의존은 `external.llm`을 통해서만 잡힌다.

## 검증 계획

- `./gradlew -p qtai-server test`
- `./gradlew -p qtai-server build`
- 도메인 경계 테스트 또는 `rg -n "domain\\.[a-z]+\\.internal|domain\\.[a-z]+\\.web" qtai-server/src/main/java/com/qtai/domain/ai`

## PR 전 체크

- [ ] UseCase 이름이 동사 + `UseCase` 규칙을 따른다.
- [ ] record DTO에 validation annotation이 필요한 경우 `jakarta.validation`을 사용한다.
- [ ] `/ai/sessions/**`, SSE, 다중 턴 맥락 유지 계약이 없다.
