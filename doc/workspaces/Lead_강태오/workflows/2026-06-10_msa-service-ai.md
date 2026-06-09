# 2026-06-10 MSA Day2 — service-ai 추출 워크플로우

> 작업 폴더(worktree): `D:\workspace\QT-AI-ai` (브랜치 `feature/msa-ai-service`, dev-msa=2bac476 기준)
> 병행: PR#2 qt·study=`QT-AI-2nd-Team-Project`, service-user=`QT-AI-day2`, service-note=`QT-AI-note`. 폴더 분리 worktree(`gradlew --stop` 금지).
> 기준: `2026-06-09_msa-restart-plan.md` §4 Day2. **Day2에서 가장 큰 작업(ai 157파일 + Kafka).**

## 대상 (사전 점검)

| 도메인 | 파일 | cross-domain | 특이 |
|--------|------|--------------|------|
| ai(157) | audit(8)·study(6)·qt(4)·bible(2)·admin(2) | 전부 다른 서비스 → Mock | Kafka 워커(AiGenerationJobWorker)·스케줄러(AiDailyQtVerseExplanationSeedScheduler)·LLM external(external/llm) |

→ cross-domain Mock 5종: **audit·study·qt·bible·admin** (`client/{도메인}/...UseCaseMock`, 통합 시 RestClient).
→ Kafka는 이 서비스에만. outbox→Kafka relay 패턴.

## TODO

- [x] **Day2-5-1 service-ai 스켈레톤 + 빌드** — settings include + boot app(web+jpa) + 스모크. `:service-ai:build` 통과 (`0caa133`)
- [ ] **Day2-5-2 ai api/internal 골격 이전** — 도메인 엔티티/리포지토리/서비스 + cross-domain Mock 5종. Kafka 의존(spring-kafka) 추가. LLM external은 external.llm 영역, API key는 env(로그 금지).
- [ ] **Day2-5-3 Kafka 워커·스케줄러 이전** — AiGenerationJobWorker(outbox→relay), 시드 스케줄러(@ConditionalOnProperty로 테스트 비활성).
- [ ] **Day2-5-4 web 컨트롤러 이전** — AiController(F-15 단발 Q&A: 단어·시대상·역사배경만, single-turn), Admin/System 컨트롤러는 노출 범위 검토(관리자분은 admin-server 소관 가능).
- [ ] **Day2-5-5 테스트 + PR** — 금지 부재 테스트(자유챗봇/다중턴/SSE/RAG 없음), F-15 차단·검증·실패 처리, 승인 안 된 산출물 미노출, ai_generation_jobs/assets/validation_logs 기록. MockMvc·단위·ArchUnit. base dev-msa.

## 핵심 원칙 (PR#1 교훈 + AI 규칙)

- 브랜치 `feature/` prefix. commit/push 분리. `gradlew --stop` 금지.
- AI 규칙(CLAUDE.md §7·§8): 허용 흐름은 사전 생성/검증 + F-15 단발 Q&A뿐. 외부 AI 원문은 검증 전 미반환. API key 로그 금지. 금지(자유챗봇/SSE/RAG/vector DB) 임시 구현도 금지.
- 첫 푸시부터 APPROVE 품질: MockMvc·단위·ArchUnit(타 도메인 internal import 금지 패턴)·표준 페이징·광범위 catch 금지·로그 민감정보 금지.

## 진행 메모
(작업하며 갱신)
