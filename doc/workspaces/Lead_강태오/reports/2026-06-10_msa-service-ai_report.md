# 작업 리포트 — MSA Day2 service-ai

- **일자**: 2026-06-10
- **작업 폴더(worktree)**: `D:\workspace\QT-AI-ai` (브랜치 feature/msa-ai-service, dev-msa=2bac476 기준)
- **작업자**: 강태오(Lead, AI 보조)
- **관련**: `HANDOFF_2026-06-10_msa-day2.md`(세션 종합 핸드오프), `2026-06-10_msa-service-ai.md`(워크플로우)

## 1. 배경
Day2 4서비스 중 service-ai(ai 도메인) 추출. PR#1(멀티모듈+lib-common+service-bible)은 dev-msa 머지 완료(2bac476). service-user/note/PR#2(qt·study)는 별도 세션 병행(폴더 분리 worktree).

## 2. 수행 내용
- **Day2-5-1 스켈레톤**: ✅ (`0caa133`) boot app(web+jpa, H2/MySQL) + 부팅 스모크.
- **Day2-5-2 도메인 이전 + Mock**: ✅ ai 도메인 전체(api/internal/web/client 157) + `external/llm`을 모듈로 복사(Strangler, 모놀리식 원본 유지). 호출하는 5개 도메인(audit·study·qt·bible·admin)의 `api` 계약을 반입하고, 실제 주입되는 UseCase 7종에 `client/{도메인}/...UseCaseMock` 제공(audit=no-op, admin=차단 throw, study·qt·bible=무해 stub). 의존은 pdfbox만 추가.
- **Day2-5-3 스케줄러·LLM**: ✅ `@Scheduled` 워커·시더는 `SchedulingConfig(@ConditionalOnProperty ai.scheduling.enabled=true)`로 게이트(기본 false → 테스트·기동 비활성). `external/llm`(DeepSeek) 이전, `@Value` 5개 키를 application.yml에 env+기본값으로 제공(api-key 로그 금지).
- **Day2-5-4 web + Security**: ✅ ai/web 전체 이전. SecurityConfig(`/api/v1/admin/**` denyAll, `@EnableMethodSecurity`)·JpaAuditingConfig(공통 Clock). 부팅 스모크 통과.
- **Day2-5-5 테스트**: ✅ 4종(13 tests). DomainBoundary(ArchUnit), AiForbiddenFeature(금지기능 부재), SecurityFilterChain(MockMvc), AiJsonStorageGuard(원문 저장거부).

## 3. 검증 결과 (2~3회 교차)
- `:service-ai:compileJava` 통과 → `:service-ai:test` 부팅 스모크 통과 → `:service-ai:build` 전체(컴파일+13 tests+bootJar) **BUILD SUCCESSFUL**.
- 부팅 시 JPA EntityManagerFactory + H2(Hikari) + 71개 internal 빈 + 22개 컨트롤러 + Mock + LLM 클라이언트 + 보안이 모두 로드 후 정상 종료 → ai 도메인이 독립 서비스로 동작 확인.

## 4. 중요한 설계 판단·이슈
- **Kafka 미사용(정정)**: 회의/메모상 "Kafka는 AI만"이지만, baseline(2bac476)의 AI 생성 파이프라인은 **`@Scheduled` 폴링(outbox)** 으로 구현돼 있어 **spring-kafka 의존을 추가하지 않았다**. Kafka 전환은 후속 과제로 명시. (근거 없는 의존 추가 회피)
- **F-15 Q&A**: baseline에 `RequestAiQaUseCase`/`GetAiQaResultUseCase` **계약만 존재하고 구현·컨트롤러는 placeholder**. 없는 기능을 지어내지 않고 충실히 이전하되, 테스트로 금지기능 부재(SSE·다중턴·세션·자유챗)를 강제.
- **관리자 AI 경로**: ai/web의 Admin 컨트롤러는 함께 이전하되 SecurityConfig `denyAll`로 차단(관리자 기능은 admin_role 이중검증 포함 admin-server 소관). PR#1에서 확립한 패턴 재사용.
- 빌드 중 막힘 2건 해결: ① `@ConditionalOnMissingBean`을 `@Component`에 달아 빈 미등록 → 순수 `@Component`로 변경, ② PowerShell `Set-Content`의 BOM 삽입 → Write 도구로 재작성.

## 5. 다음 단계
- `feature/msa-ai-service` → dev-msa PR. 필수체크(qtai-server Build & Test / Flutter / no-cross-merge) green 확인 → claude-review APPROVE 시 자동 squash.
- 통합 단계: cross-domain UseCaseMock 7종을 RestClient 어댑터로 교체하고 Mock 삭제. AI 생성 파이프라인 Kafka 전환 검토.
