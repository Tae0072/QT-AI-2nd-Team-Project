# Report - 2026-05-20 ai-usecase-contracts

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강상민 |
| 브랜치 | `feature/ai-usecase-contracts` |
| 관련 workflow | `doc/workspaces/DevC_강상민/workflows/2026-05-20_ai-usecase-contracts.md` |
| 관련 F-ID | F-02, F-14, F-15 |
| 실행 경로 | 직접 실행 |

## 작업 결과

AI 도메인의 공개 UseCase 계약을 자유 응답형 계약에서 목적별 계약으로 분리했다. 다른 도메인이 `domain.ai.api`의 interface와 command/result record만 바라보도록 하고, QT 컨텍스트 조회는 `domain.ai.client.qt` adapter 경계로 분리했다.

## 변경 요약

1. 기존 자유 응답형 계약인 `GenerateAiResponseUseCase`, `AiPromptRequest`, `AiResponse`를 제거했다.
2. F-15 단발 Q&A 계약을 `RequestAiQaUseCase`, `GetAiQaResultUseCase`로 분리했다.
3. 해설·시뮬레이터 사전 생성 경로를 `CreateAiGenerationJobUseCase`로 분리했다.
4. 산출물 등록, 검증 로그 등록, 관리자 리뷰 계약을 각각 `RegisterAiGeneratedAssetUseCase`, `RegisterAiValidationLogUseCase`, `ReviewAiAssetUseCase`로 추가했다.
5. `api/dto`는 HTTP Request/Response가 아니라 `Command` / `Result` record 명칭만 사용하도록 정리했다.
6. QT 컨텍스트 조회 mock adapter를 `QtContextClient`, `QtContextResult`, `GetQtUseCaseMock` 구조로 정리했다.
7. `AiController`, `AiService`의 오래된 자유 응답 계약 주석을 단발 Q&A와 사전 생성 UseCase 기준으로 정리했다.

## 테스트 보강

| 테스트 파일 | 검증 내용 |
| --- | --- |
| `AiUseCaseContractTest` | 공개 AI 계약이 UseCase interface인지, method가 Command/Result record만 쓰는지, legacy 자유 응답 계약이 제거됐는지 검증 |
| `AiQtClientContractTest` | AI 도메인이 QT 컨텍스트를 client adapter 경계로 조회하고 본문 원문 필드를 노출하지 않는지 검증 |

## 검증 결과

| 명령 | 결과 |
| --- | --- |
| `gradle test --tests "*AiUseCaseContractTest" --tests "*AiQtClientContractTest"` | 통과 |
| `gradle clean test` | 통과 |
| `gradle build` | 통과 |
| `rg -n "GenerateAiResponseUseCase|AiPromptRequest|AiResponse" qtai-server/src/main/java/com/qtai/domain/ai` | 매치 없음 |
| `rg -n "import com\\.qtai\\.domain\\.[^.]+\\.(internal|web)" qtai-server/src/main/java/com/qtai/domain/ai qtai-server/src/main/java/com/qtai/external/llm` | 매치 없음 |
| `git diff --check` | 공백 오류 없음. CRLF 변환 경고만 출력됨 |

## 범위 확인

- 실제 관리자 HTTP API 구현은 하지 않았다.
- 실제 LLM provider 요청/응답 매핑은 하지 않았다.
- 타 도메인의 Entity, Repository, Service는 수정하지 않았다.
- `external.llm` 계약은 기존 provider-neutral port를 유지했고, 실제 provider 구현은 변경하지 않았다.

## 주의 사항

현재 작업트리에 `qtai-server/src/main/java/com/qtai/common/dto/ApiResponse.java`, `qtai-server/src/main/java/com/qtai/common/exception/ErrorCode.java`의 unused import 삭제가 함께 남아 있다. 이 두 파일은 이번 workflow 담당 경로 밖 변경이므로 PR 범위에 포함할지 별도 확인이 필요하다.

## 후속 작업

1. `ai-pre-generation-validation` workflow에서 batch/admin 사전 생성 경로 구현
2. `ai-f15-qa-request-flow` workflow에서 F-15 요청/조회 API와 검증 흐름 구현
3. `ai-admin-review-regeneration` workflow에서 관리자 승인·반려·숨김·재생성 구현
4. 실제 QT 도메인 UseCase가 준비되면 `GetQtUseCaseMock`을 실 adapter로 교체
