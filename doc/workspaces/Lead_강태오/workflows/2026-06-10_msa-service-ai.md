# 2026-06-10 MSA Day2 — service-ai 추출 워크플로우

> 작업 폴더(worktree): `D:\workspace\QT-AI-ai` (브랜치 `feature/msa-ai-service`, dev-msa=2bac476 기준)
> 병행: PR#2 qt·study=`QT-AI-2nd-Team-Project`, service-user=`QT-AI-day2`, service-note=`QT-AI-note`. 폴더 분리 worktree(`gradlew --stop` 금지).
> 기준: `2026-06-09_msa-restart-plan.md` §4 Day2. **Day2에서 가장 큰 작업(ai 157파일 + Kafka).**

## 대상 (사전 점검)

| 도메인 | 파일 | cross-domain | 특이 |
|--------|------|--------------|------|
| ai(157) | audit(8)·study(6)·qt(4)·bible(2)·admin(2) | 전부 다른 서비스 → Mock | Kafka 워커(AiGenerationJobWorker)·스케줄러(AiDailyQtVerseExplanationSeedScheduler)·LLM external(external/llm) |

→ cross-domain Mock: **audit·study·qt·bible·admin** (`client/{도메인}/...UseCaseMock`, 통합 시 RestClient). 실제 주입되는 UseCase는 7종(admin 1·audit 1·study 3·qt 1·bible 1).
→ ⚠️ **정정**: baseline(dev-msa=2bac476)의 AI 생성 파이프라인은 **Kafka가 아니라 `@Scheduled` 폴링(outbox) 방식**으로 구현돼 있다(`AiGenerationJobWorker`=`@Scheduled(fixedDelay)`, 시더=`@Scheduled(cron)`). 따라서 **spring-kafka 의존은 추가하지 않았다**(Kafka 도입은 후속). 추가 의존은 pdfbox(검증 참조자료 PDF 텍스트 추출)뿐.

## TODO

- [x] **Day2-5-1 service-ai 스켈레톤 + 빌드** — settings include + boot app(web+jpa) + 스모크. `:service-ai:build` 통과 (`0caa133`)
- [x] **Day2-5-2 ai api/internal 이전 + Mock** — ai 도메인 전체(api/internal/web/client 157) + external/llm 복사. 5개 도메인 api 계약 반입. cross-domain UseCaseMock 7종(audit no-op / admin 차단throw / study·qt·bible benign stub). pdfbox 추가. LLM 설정은 env+기본값(api-key 로그 금지). 컴파일 통과.
- [x] **Day2-5-3 스케줄러·LLM external** — `@Scheduled` 워커·시더는 `SchedulingConfig(@ConditionalOnProperty ai.scheduling.enabled, 기본 false)`로 게이트해 테스트·기동에서 비활성. `external/llm`(DeepSeek) 이전, `@Value` 키 5종 application.yml에 env 주입. (Kafka 해당 없음)
- [x] **Day2-5-4 web 컨트롤러 + Security** — ai/web 전체 이전(AiController는 baseline에서 미구현 placeholder라 그대로 유지; F-15 구현체 부재). SecurityConfig(`/api/v1/admin/**` denyAll, `@EnableMethodSecurity`), JpaAuditingConfig(Clock). `:service-ai:test` 부팅 스모크 통과. (`a82724b`→ 이번 커밋들)
- [x] **Day2-5-5 테스트** — DomainBoundary(ArchUnit)·AiForbiddenFeature(SSE/세션/스트리밍/자유챗 경로·반환타입 부재)·SecurityFilterChain(MockMvc, admin denyAll)·AiJsonStorageGuard(원문·참조자료 저장거부) 4종, 13 tests green.
- [ ] **PR** — `feature/msa-ai-service` → dev-msa. 필수체크 green → claude-review APPROVE → 자동 squash.

## 핵심 원칙 (PR#1 교훈 + AI 규칙)

- 브랜치 `feature/` prefix. commit/push 분리. `gradlew --stop` 금지.
- AI 규칙(CLAUDE.md §7·§8): 허용 흐름은 사전 생성/검증 + F-15 단발 Q&A뿐. 외부 AI 원문은 검증 전 미반환. API key 로그 금지. 금지(자유챗봇/SSE/RAG/vector DB) 임시 구현도 금지.
- 첫 푸시부터 APPROVE 품질: MockMvc·단위·ArchUnit(타 도메인 internal import 금지 패턴)·표준 페이징·광범위 catch 금지·로그 민감정보 금지.

## 진행 메모

- 2026-06-10: ai 도메인 추출 본체 완료. 커밋 흐름:
  - 도메인 이전 + Mock + config: `feat(msa): service-ai에 ai 도메인 전체 이전 ...`
  - 테스트: `test(msa): service-ai 테스트 — 도메인경계·AI금지기능부재·보안·원문저장가드`
- 막혔던 지점 2건(해결):
  1. `@ConditionalOnMissingBean`을 일반 `@Component` Mock에 달았더니 컴포넌트 스캔 순서 의존으로 빈이 등록되지 않아 `NoSuchBeanDefinitionException`(WriteAuditLogUseCase). → 순수 `@Component`로 변경(통합 시 Mock 삭제 방식, CLAUDE.md §4).
  2. PowerShell `Set-Content -Encoding UTF8`이 BOM을 넣어 Java 컴파일 실패(`illegal character ﻿`). → Write 도구(BOM 없음)로 재작성. (메모리에 기록된 알려진 이슈)
- 검증: `:service-ai:compileJava`→`:service-ai:test`(부팅 스모크)→`:service-ai:build`(13 tests) 3중 통과. IDE 인덱싱 없는 worktree라 build 잠금은 모듈 `build/` 폴더 삭제 후 재빌드로 해소.
