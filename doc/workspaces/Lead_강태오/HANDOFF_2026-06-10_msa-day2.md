# 🔁 세션 핸드오프 — QT-AI MSA 전환 (2026-06-09~10)

> 목적: 이 문서 하나로 다른 세션이 지금까지의 모든 작업·결정·규칙·다음 할 일을 파악하고 이어서 진행한다.
> 읽는 순서: 0(맥락) → 4(현재 상태) → 5·6(규칙·결정) → 9(이 세션의 남은 일=service-ai) → 10(이어받기).

---

## 0. 프로젝트 한 줄 요약

- QT-AI = 큐티(성경 묵상) 앱. 백엔드 모놀리식 `qtai-server`(Java 21 / Spring Boot 3.3 / Gradle 9.5.1 / Spring Modulith).
- 작업 저장소: `Tae0072/QT-AI-2nd-Team-Project`. 문서 저장소: `Tae0072/2nd-Team-Project`.
- 규칙 원문은 각 작업폴더 루트의 `CLAUDE.md`(반드시 우선 준수). 회의 결정 원본: Notion 2026-06-09 MSA 회의록.

## 1. 큰 그림 (지금 하는 일)

모놀리식 `qtai-server`를 **Strangler 방식으로 Gradle 멀티모듈 MSA**로 전환 중. 회의(2026-06-09) 확정 설계:
- 사용자 서비스 4개 = **유저(member·notification·mission) / 성경(bible·qt·study·music·praise) / 노트(note·sharing·report제출) / AI(ai)** + **관리자 서버(모놀리식)** + **단일 DB**(도메인=테이블).
- 서비스 간 통신 **RestClient 동기만**(읽기 중심 → Saga/보상트랜잭션 불필요). **Kafka는 AI에만**.
- JWT는 **service-user가 발급(개인키)**, 나머지는 **검증만(공개키, lib-common)**.
- 관리자 기능은 admin-server 소관(콘텐츠/사용자 서비스에선 `/api/v1/admin/**` denyAll).
- QT는 날짜별 JSON을 오브젝트 스토리지(S3 API; 로컬 MinIO/배포 S3·R2)에 — 후속.
- 배치: 모노레포 + 서비스 폴더(별도 레포 X). 로컬은 쿠버네티스 배포로 마무리(Day3).

## 2. 이 세션이 한 일 (시간순 요약)

1. **dev-msa 리셋**: 이전 MSA Phase 1(DB-per-service·CircuitBreaker 등 71커밋)이 새 설계와 반대라 `dev-msa`를 `origin/dev`(#340)로 hard reset + force-push. 폐기분은 태그 `archive/dev-msa-before-reset-20260609`(=95ecfb4)로 보존. (force-push는 보호브랜치라 gh로 allow_force_pushes 잠깐 켜고→push→복구)
2. **PR #1 작성·머지**: 멀티모듈 골격 + lib-common + service-bible(bible·music·praise). dev-msa에 squash 머지 완료(merge `2bac476`, PR #422). 자동리뷰 지적(보안 permitAll·광범위 catch·ApiResponse fields·테스트 부족)을 보강 후 머지.
3. **dev-admin-web 점검**: 백엔드는 dev와 동일한 풀 모놀리식, 차이는 admin-web(React)뿐. admin-server는 dev-msa(백엔드)에서 만들기로(머지 충돌 방지).
4. **병렬화**: 큰 작업을 worktree 폴더로 분리해 세션별 병렬. (아래 4번)
5. **Day2 시작**: service-user·service-note·service-ai 스켈레톤 생성, 각 워크플로우 문서화.

## 3. 자동머지(claude-review) 작동·교훈 ★중요

- dev-msa 보호규칙 = **필수 체크 3개**(`no-cross-merge`, `qtai-server Build & Test`, `Flutter Analyze & Test`) + **승인 0개**.
- `claude-pr-review.yml`이 PR을 리뷰 → **마지막 줄이 `APPROVE`이고 전 CI green이면 자동 squash 머지**. `REQUEST_CHANGES`는 COMMENT로 바뀌어 비차단(필수체크만 green이면 수동 머지 가능).
- **한 방 자동머지가 잘 안 되는 이유 2가지**: ① 브랜치명 `feat/`는 CI 실패 → 반드시 **`feature/`**(허용: feature|bugfix|hotfix|chore|release|docs|test). ② claude-review(~2.5분)가 빌드(~3.5분)보다 먼저 끝나 auto-merge 단계에서 "빌드 진행 중"으로 스킵 → 빌드 green 후 `gh run rerun <claude-review run id>`로 재실행.
- 그래서 **첫 푸시부터 APPROVE 품질**로 올리는 게 핵심(아래 8·9의 인수조건).

## 4. 현재 4-트랙 상태 (worktree 폴더 분리 = 빌드 충돌 없음)

| 트랙 | 폴더 | 브랜치 | 상태 |
|------|------|--------|------|
| PR#2 qt·study | `D:\workspace\QT-AI-2nd-Team-Project` | feature/msa-qt-study | 다른 세션 진행 중(컨트롤러 테스트·DomainBoundary 개선 등) |
| service-user(member·notification·mission) | `D:\workspace\QT-AI-day2` | feature/msa-user-service | 다른 세션, member·JWT발급·Kakao·Redis 진행 중 |
| service-note(note·sharing·report제출) | `D:\workspace\QT-AI-note` | feature/msa-note-service | 스켈레톤 완료, 다른 세션 대기 |
| **service-ai(ai+Kafka)** | **`D:\workspace\QT-AI-ai`** | **feature/msa-ai-service** | **스켈레톤 완료 — 이 핸드오프 대상** |

- 기준선: `origin/dev-msa = 2bac476`(PR#1 머지본). 모든 worktree가 여기서 분기.
- 각 worktree에 그 트랙의 워크플로우 문서가 `doc/workspaces/Lead_강태오/`에 있음.

## 5. 공통 규칙·환경 (반드시 지킬 것)

- **git/gh는 호스트(Windows)에서**: Windows-MCP PowerShell로 `D:\Git\cmd\git.exe`·`gh`(Tae0072 인증). **샌드박스(bash) git은 이 마운트 repo config를 못 읽음** → 깃 작업은 PowerShell로.
- **빌드**: 각 worktree의 `qtai-server`에서 호스트 `gradlew.bat`로 `:service-xxx:build`. 백그라운드(Start-Process)+로그 폴링 권장(빌드 길면 MCP 타임아웃).
- **`gradlew --stop` 금지**: 데몬 전역 종료 → 다른 세션 빌드 중단. (worktree는 IDE 인덱싱 안 해 build 잠금 거의 없어 --stop 불필요)
- **build 폴더 잠금**(`Unable to delete/Failed to clean up stale outputs`)이 나면: 그 모듈 `build/` 폴더 삭제 후 재빌드(메인폴더에서만 가끔 발생).
- **commit과 push는 분리 호출**: 체이닝하면 `.git/worktrees/* 권한경고`로 exit→push 누락됨. (이 권한경고 자체는 무해)
- **브랜치 `feature/` prefix, Conventional Commits, PR base=dev-msa.**

## 6. 확정 아키텍처 결정 (요약)

- 단일 DB(H2 로컬·MySQL env, ddl-auto 기본 `validate`·테스트만 `create-drop` override). DB-per-service 금지.
- RestClient 동기 통신, Kafka는 AI만. JWT 발급=service-user, 검증=lib-common.
- Strangler: root 모놀리식 `qtai-server/src`는 그대로 두고 모듈로 점진 추출(전환 중 코드 중복 정상). 추출 완료 후 모놀리식 제거.
- 멀티모듈 플러그인 버전은 `settings.gradle.kts` pluginManagement에서 공유.

## 7. lib-common 제공물 (PR#1, 재사용)

`com.qtai.common.*` 동일 패키지로 제공 → 도메인 코드 이전 시 import 무수정:
- `dto.ApiResponse`(success/data/error{code,message,fields}/timestamp/traceId), `dto.ApiResponse.FieldError`
- `exception.{ErrorCode, BusinessException, GlobalExceptionHandler}`
- `entity.BaseEntity`(JPA MappedSuperclass, auditing)
- `security.{JwtValidator(공개키 검증), JwtAuthenticationFilter(@ConditionalOnProperty security.jwt.public-key), SecurityErrorResponseWriter}`
- `config.{RestClientConfig, TimeConfig(Clock Asia/Seoul)}`

## 8. 서비스 추출 표준 절차 (service-bible 사례)

1. **스켈레톤**: settings include + 모듈 build.gradle.kts(`project(":lib-common")` + web/jpa/cache/h2/mysql/test/archunit/security-test) + `XxxServiceApplication`(@SpringBootApplication(scanBasePackages="com.qtai") + @EntityScan/@EnableJpaRepositories("com.qtai.domain") + @EnableCaching) + application.yml(포트·H2) + 부팅 스모크(@SpringBootTest, ddl-auto create-drop override). → `:service-xxx:build` 통과.
2. **도메인 이전**(Strangler): 모놀리식 `src/.../domain/<d>`를 모듈로 **복사**(같은 패키지). 같은 서비스 도메인끼리는 in-process, 다른 서비스 도메인은 `client/{도메인}/...UseCaseMock`(상대 `api` 계약만 가져옴, internal import 금지).
3. **부가 설정**: BaseEntity 쓰면 `@EnableJpaAuditing`+`DateTimeProvider(Clock)`를 별도 `JpaAuditingConfig`로. `Clock` 빈은 lib-common 제공. `SecurityConfig`(@EnableWebSecurity+@EnableMethodSecurity, `/api/v1/admin/**` denyAll, 그외 authenticated, JWT 필터 ObjectProvider로 조건부 체인).
4. **테스트**(첫 푸시 APPROVE용): 도메인 서비스 단위테스트(Mockito), **Controller MockMvc 통합테스트**(미인증 401/403·정상 200), **ArchUnit DomainBoundaryTest**(=service-bible 패턴: "타 도메인 internal import 금지" ArchCondition, com.qtai.domain 대상), 광범위 catch(Exception) 금지, 로그 민감정보 금지, 표준 페이징 envelope.
5. **PR**: `feature/...`→dev-msa, Conventional title. 빌드 green 후 자동머지/재실행.

## 9. 이 세션의 남은 작업 = service-ai 본체 추출 ★

폴더 `D:\workspace\QT-AI-ai`, 브랜치 `feature/msa-ai-service`. 스켈레톤(Day2-5-1)은 완료·커밋(`0caa133`)·빌드 통과. 워크플로우 TODO는 `doc/workspaces/Lead_강태오/workflows/2026-06-10_msa-service-ai.md`.

- 대상: `ai`(157파일). cross-domain = audit·study·qt·bible·admin(전부 다른 서비스 → `client/{도메인}/UseCaseMock`).
- 특이: **Kafka**(AiGenerationJobWorker, outbox→relay) — `spring-kafka` 추가. **스케줄러**(AiDailyQtVerseExplanationSeedScheduler, `@ConditionalOnProperty`로 테스트 비활성). **LLM external**(`external/llm`) — API key는 env, 로그 금지.
- **AI 규칙(CLAUDE.md §7·§8)**: 허용 흐름은 사전 생성/검증 + **F-15 단발(single-turn) 사실 Q&A뿐**(단어·시대상·역사배경). 외부 AI 원문은 검증 전 미반환. **금지(임시 구현도 금지)**: 자유챗봇·다중턴·SSE·`/ai/sessions/**`·RAG·vector DB. ai_generation_jobs/ai_generated_assets/ai_validation_logs 기록.
- TODO: 5-2 ai api/internal+Mock(+spring-kafka) → 5-3 Kafka 워커·스케줄러 → 5-4 web(AiController F-15만; Admin/System 컨트롤러 노출범위 검토) → 5-5 테스트(금지 부재·F-15 차단/검증/실패·미승인 산출물 미노출)+PR.
- 인수조건(첫 푸시 APPROVE): 8-4의 테스트 세트 + AI 금지기능 부재 테스트.

## 10. 이어받기 절차 (새 세션)

1. 새 세션에 폴더 `D:\workspace\QT-AI-ai` 연결.
2. 메모리의 "QT-AI MSA Phase 1", "QT-AI build & git toolchain" + 이 문서(HANDOFF) + `workflows/2026-06-10_msa-service-ai.md`를 읽는다.
3. 다른 worktree(QT-AI-2nd-Team-Project / QT-AI-day2 / QT-AI-note)는 **건드리지 말 것**(각 세션 소유).
4. service-ai Day2-5-2부터 진행 → 빌드(`:service-ai:build`, --stop 금지) → 2~3회 검토 → PR(base dev-msa).
5. 작업 내용은 `doc/workspaces/Lead_강태오/`의 service-ai 워크플로우·리포트·스터디노트에 갱신.

---
끝. (이 문서는 feature/msa-ai-service에 커밋됨. dev-msa 머지 전까지는 이 브랜치/폴더에서 참조)
