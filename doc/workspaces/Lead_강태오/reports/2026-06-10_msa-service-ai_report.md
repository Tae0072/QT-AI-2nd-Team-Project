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

## 4-1. 테스트 커버리지·누락 사유 (v3.1 기준 명시)

claude-review 1차(REQUEST_CHANGES) 지적을 반영해 다음을 보강했다.

**보강(추가) 테스트**
- `DeepSeekLlmClientTest`(6): 정상 파싱 / 429→LLM_RATE_LIMIT / 타임아웃→LLM_TIMEOUT / api-key 미설정→LLM_CONFIGURATION_ERROR / 비정상 응답→LLM_RESPONSE_INVALID / 외부오류 INTERNAL_ERROR 매핑.
- `AiServiceTest`(3): SIMULATOR 생성 즉시 거부(저장소 미접근 검증) / null command / 미지원 jobType.
- 보안 보강: `VerifyAdminRoleUseCaseMock`을 `BusinessException(ADMIN_ROLE_INSUFFICIENT, 403)`으로 변경(인가 경로 500 누출 방지).

**이번 PR에서 의도적으로 보류한 테스트(사유)**
- 본 PR은 **구조 이전(Strangler) 중심**으로, 모놀리식에서 이미 검증된 ai 도메인 로직을 패키지 무수정 복사한 것이다. 따라서 회귀 위험이 큰 영역(부팅·도메인경계·보안·금지기능·정책 가드·외부 LLM 경계)에 테스트를 집중했다.
- 아래는 **통합 단계(Mock→RestClient 교체) PR에서 함께 보강**한다. 그 전에는 cross-domain Mock이 stub이라 행위 검증의 의미가 제한적이기 때문이다:
  - `AiAssetReviewService` 승인·반려·숨김 + APPROVED 게이트(layer1/2 PASSED) 단위테스트
  - `AiAutoValidationService` payload schema/verse scope/forbidden fields 분기
  - `AiGenerationJobRunner.sweepStaleRunningJobs`(P1-3) 회수
  - `Admin*Controller` MockMvc 통합(현재 service-ai에서 `/api/v1/admin/**` denyAll로 차단되어 admin-server 이관 시 그쪽에서 검증)
- 위 보류는 `25_기능_명세서`/`18_코드_품질_게이트` 위반이 아니라, MSA 분리 PR의 범위 한정(구조 이전)에 따른 단계적 보강 계획이다.

## 5. 다음 단계
- `feature/msa-ai-service` → dev-msa PR. 필수체크(qtai-server Build & Test / Flutter / no-cross-merge) green 확인 → claude-review APPROVE 시 자동 squash.
- 통합 단계: cross-domain UseCaseMock 7종을 RestClient 어댑터로 교체하고 Mock 삭제. AI 생성 파이프라인 Kafka 전환 검토.
