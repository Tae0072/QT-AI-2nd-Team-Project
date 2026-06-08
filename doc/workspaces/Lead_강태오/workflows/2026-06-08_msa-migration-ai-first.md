# 2026-06-08 · 모놀리식 → MSA 전환 (ai 도메인 우선) — 작업 진행 순서 워크플로우

작성: Claude (Lead 강태오/T 지시) · 대상 저장소: `QT-AI-2nd-Team-Project` (구현)

## 1. 요약 (한 줄)

단일 `qtai-server` 모듈러 모놀리식을 **Strangler Fig 방식**으로 점진 분리한다. 1차로 **ai 도메인**을 별도 서비스(`qtai-ai-service`)로 떼어내고, 서비스 간 통신은 **동기 REST + Kafka 이벤트**, DB는 **단계적 분리**, 배포는 **Docker Compose 먼저 → 이후 Kubernetes/Helm**으로 간다.

> 입문자용 한 줄 설명: "한 덩어리 서버"에서 AI 부분만 먼저 "독립 서버"로 떼어내고, 나머지는 그대로 둔 채 천천히 하나씩 분리하는 계획이다. 한 번에 다 쪼개지 않으므로 문제가 생겨도 되돌리기 쉽다.

---

## 2. 전제 · 요구사항 변경 결정 (가장 중요)

현재 **모든 기준 문서가 "단일 qtai-server 모듈러 모놀리식"을 못박고, Kafka·Kubernetes·Helm을 v1 금지**로 규정한다. MSA 전환은 이 결정을 정면으로 바꾸는 일이라, **코드보다 먼저 거버넌스(문서·CI 게이트) 변경이 선행**되어야 한다.

### 2.1 변경 결정 (요약)

- **결정:** v1 종료 시점부터 **v2 = MSA**로 전환을 시작한다. 1차 대상은 ai 도메인.
- **승인 주체:** Lead(강태오) / 강사님 검토 기록 필요. ※ 이 문서는 그 결정을 실행 계획으로 옮긴 것이며, **정식 승인 기록(회의록·문서 PR)이 남기 전에는 Phase 2 이후 코드 작업을 시작하지 않는다.**
- **선결 조건:** v1 MVP 기능이 동결(freeze)되어 있어야 한다. 미완 기능이 있으면 그 마감이 우선이다.

### 2.2 풀어야 할 "금지" 항목 — 이걸 안 풀면 CI가 빌드를 막는다

CI에 금지기술을 **자동 차단하는 게이트가 실제로 살아있다.** 아래를 먼저 수정하지 않으면 Kafka/K8s 코드를 올리는 순간 모든 PR이 실패한다.

| 파일 | 현재 동작 | 필요한 변경 |
|---|---|---|
| `.github/workflows/qt-ai-ci.yml` (134~143행) | `KafkaTemplate\|spring-kafka\|@KafkaListener\|org.apache.kafka` 발견 시 **[BLOCK]**, `k8s/`·`helm/` 디렉터리 발견 시 **[BLOCK]** | v2 전환 범위에 한해 허용하도록 게이트 완화/조건화 |
| `.github/workflows/claude-pr-review.yml` (133행) | 자동 리뷰가 Kafka/K8s/Helm을 "금지 인프라"로 지적 | 리뷰 기준 프롬프트에서 v2 예외 반영 |

### 2.3 갱신해야 할 기준 문서 (문서 저장소)

아래 문서가 "모놀리식 고정 / Kafka·K8s·Helm 금지"를 담고 있어, 전환 결정을 반영해야 일관성이 유지된다. **문서 PR을 별도로 진행**한다(코드 PR과 분리).

- `03_아키텍처_정의서.md` §247 — 이미 "v2 마이크로서비스 분리 대비"를 명시. 이 문장을 **정식 v2 아키텍처 섹션으로 승격**(목표 서비스맵·통신·데이터·인프라 추가).
- `01_프로젝트_계획서.md`(93/97/169/170), `09_Git_규칙.md`(21/121/290/350), `10_환경_설정.md`(44), `12_코드_리뷰_규칙.md`(44), `14_배포_가이드.md`(26), `18_코드_품질_게이트.md`(67/79), `20_CICD_파이프라인.md`(34/114/120/219), `22_구현_저장소_반영_체크리스트.md`(160/204) — 금지 목록에서 Kafka/K8s/Helm을 **"v1 금지 → v2 허용"으로 조건화**.
- `00_개발_일정_총괄표.md` — v2 전환 마일스톤/일정 추가.
- 구현 레포 `CLAUDE.md` §1·§8 — 서버 형태 및 금지기술 항목 갱신.

> 정렬 원칙: 충돌 시 판단 순서는 `07 → 03 → 04 → 18 → 23 → 09`. 이번 전환은 `03`을 중심으로 정의하고 나머지를 맞춘다.

---

## 3. 목표 아키텍처 (1차 분리 후 모습)

```
        [ Flutter 앱 ]        [ 관리자 웹 ]
              │                    │
              ▼                    ▼
        ┌─────────────────────────────────┐
        │        API Gateway              │   /api/v1/ai/**, /api/v1/admin/ai/**  → ai-service
        │   (Spring Cloud Gateway 등)     │   그 외 모든 경로                      → monolith
        └─────────────────────────────────┘
              │                          │
              ▼                          ▼
   ┌────────────────────┐      ┌──────────────────────┐
   │  qtai-server        │◀────▶│  qtai-ai-service      │
   │  (나머지 12 도메인) │ REST │  (ai 도메인)          │──▶ DeepSeek(LLM)
   └────────────────────┘      └──────────────────────┘
            │   ▲                    │   ▲
            └───┴──── Kafka 이벤트 ───┴───┘   (qt.passage.ready / ai.asset.approved 등)
            │                            │
       [ MySQL: 공유 → 단계적 분리 ]  [ ai 전용 스키마(분리 단계) ]
```

- **변하지 않는 핵심:** 도메인 간 호출은 지금도 `api/UseCase` 인터페이스 + DTO로만 한다. MSA에서는 **그 인터페이스 뒤의 구현만 "인프로세스 호출 → REST/이벤트"로 교체**한다. 즉 계약(인터페이스)은 유지하고 배선만 바꾸는 게 Strangler의 핵심이다.
- 실제 가로채기 지점: 이미 존재하는 `qt/client/ai/...`(qt가 ai를 부르는 어댑터)와 같은 **client 어댑터를 REST 호출로 교체**한다.

---

## 4. 단계별 작업 순서

각 Phase는 **이전 Phase가 검증된 뒤** 시작한다. 체크박스는 완료 기준(DoD)이다.

### Phase 0 — 거버넌스 · 게이트 해제 · v1 동결 (선행, 코드 없음)
- [ ] v1 MVP 동결 지점 확정 (남은 기능 마감 여부 확인)
- [ ] Lead/강사님 전환 승인 기록 (회의록 + 문서 PR)
- [ ] §2.3 기준 문서 갱신 PR (문서 저장소)
- [ ] §2.2 CI 게이트(`qt-ai-ci.yml`, `claude-pr-review.yml`) 조건화 PR
- [ ] v2 작업 브랜치 전략 합의 (예: `dev`에서 `feat/msa-ai-extract-*`)

### Phase 1 — 기반 준비 (분리 전 토대)
- [ ] **목표 서비스맵 확정**: 1차 = ai만 분리, 나머지는 monolith 유지
- [ ] **API Gateway 도입**: ai 경로(`/api/v1/ai/**`, `/api/v1/admin/ai/**`)만 ai-service로 라우팅, 그 외 monolith
- [ ] **관측성(Observability) 기반**: 요청 상관관계 ID(correlation id) 전파, 분산 추적/로그 표준 — *서비스가 쪼개지기 전에* 깔아야 디버깅이 된다
- [ ] **공통 모듈 처리 방침**: `common`(ApiResponse·ErrorCode·BusinessException)을 공유 라이브러리로 분리할지/복제할지 결정
- [ ] **Docker Compose에 인프라 추가**: Kafka(+Zookeeper/Kraft), (게이트웨이), ai용 DB 컨테이너 자리 준비
- [ ] **계약 우선 정의**: ai-service가 노출할 REST 계약(OpenAPI) + 이벤트 스키마 초안 — 기존 `ai/api/**` UseCase 시그니처를 그대로 계약으로 승격

### Phase 2 — ai-service 추출 (공유 DB 상태로 먼저 분리)
> 목표: ai를 "별도로 배포되는 서비스"로 만들되, DB는 아직 공유해 위험을 줄인다.
- [ ] `qtai-ai-service` 모듈/배포 단위 생성, ai 도메인 코드 이동 (`domain.ai.*`, `external.llm`(DeepSeek))
- [ ] ai의 REST 컨트롤러 노출: 생성/검증(`CreateAiGenerationJobUseCase`, `RegisterAiGeneratedAssetUseCase`), F-15 Q&A(`RequestAiQaUseCase`, `GetAiQaResultUseCase`), 관리자(`ReviewAiAssetUseCase` 등 admin/asset·checklist·monitoring)
- [ ] **Strangler 가로채기**: monolith 쪽 `qt/client/ai/...` 어댑터를 **인프로세스 호출 → REST 클라이언트(RestClient/Feign) 호출**로 교체. UseCase 인터페이스 시그니처는 그대로
- [ ] 게이트웨이 라우팅 적용 후 ai 경로가 ai-service로 가는지 확인
- [ ] 외부 오류 공통 처리: 서비스 간 호출 실패를 공통 예외/응답으로 감싸기(타임아웃·재시도·서킷브레이커 기본값)
- [ ] **DomainBoundaryArchTest / ArchitectureBoundaryTest 갱신**: ai가 서비스로 빠지면 모놀리식 경계 규칙의 의미가 바뀐다 — ai 관련 규칙을 서비스 경계 기준으로 재정의

### Phase 3 — Kafka 이벤트 도입 (배치 타이밍 보존이 핵심)
> 가장 위험한 구간. **00:00 공개 / 00:05 AI 해설 job 시딩 / 04:00 노출·cache 갱신**이 서비스 경계를 넘어서도 그대로 동작해야 한다.
- [ ] 이벤트 설계: 예) `qt.passage.ready`(00:00 이후) → ai-service가 구독해 00:05 해설 job 시딩 / `ai.asset.approved` → bible·qt가 구독해 04:00 노출
- [ ] 00:05 시딩 배치의 소유 위치 결정 (monolith 스케줄러가 ai-service 호출 vs ai-service 내부 배치) — 주체는 `SYSTEM_BATCH`로 기록 유지
- [ ] 이벤트 핸들러 실패 로그(`eventId`, event type, handler name, error message) + 재처리 가능 상태 보존
- [ ] 04:00 노출 기준(승인본 `verse_explanations`만 노출, 검증 전 원문 미노출)이 서비스 분리 후에도 유지되는지 검증

### Phase 4 — ai 데이터 분리 (DB-per-service, 단계적)
- [ ] ai 소유 테이블(`ai_generation_jobs`, `ai_generated_assets`, `ai_validation_logs` 등)을 ai 전용 스키마/DB로 이전
- [ ] 교차 참조 제거: 다른 서비스가 ai 테이블을 직접 읽지 않고 **REST/이벤트로만** 접근
- [ ] `verse_explanations` 노출 데이터 소유권 정리(ai 생성 ↔ bible/qt 노출 경계)

### Phase 5 — 검증 · 안정화 (QA 트랙)
- [ ] **계약 테스트**(consumer-driven, 예: Spring Cloud Contract): monolith↔ai-service 계약 깨짐 자동 탐지
- [ ] 서비스 간 통합/스모크 테스트, 회복탄력성(타임아웃·재시도·서킷브레이커) 테스트
- [ ] 기존 필수 테스트 보존: F-15 Q&A 차단·검증, 미승인 산출물 미노출, admin 권한·`SYSTEM_BATCH`, 금지 번역본 차단
- [ ] CI에 ai-service 빌드/테스트 + 계약 테스트 스테이지 추가

### Phase 6 — Kubernetes / Helm 이행 (Compose 안정화 이후)
- [ ] ai-service·monolith·Kafka·gateway Helm 차트 작성, 서비스별 배포/롤백 절차
- [ ] 14_배포_가이드 / 20_CICD 갱신 (Compose → K8s 이중 트랙)

### Phase 7 — 다음 서비스 반복 (Strangler 반복)
- [ ] 다음 후보 평가: `notification`(주변부·비동기, 추출 쉬움) → 이후 `note/sharing/praise`(이승욱 담당, bible/qt 결합 주의) → `member`(의존 허브라 신중)
- [ ] Phase 2~5 템플릿을 그대로 반복 적용

---

## 5. 역할 분담 (사용자 제안 + 보완)

| 인원 | 트랙 | 주요 책임 |
|---|---|---|
| 강상민 | MSA — ai-service | ai 도메인 추출, REST 노출, DeepSeek 클라이언트 이전, ai DB 분리 (본인 담당 도메인이라 적합) |
| 이승욱 | MSA — 통신·이벤트·어댑터 | monolith 측 `client/ai` REST 어댑터, Kafka 이벤트 발행/구독, 배치 타이밍 보존, 다음 차수(note/sharing) 준비 |
| 강태오(Lead) | 관리자 웹 + **거버넌스/인프라 겸임** | 관리자 웹 진행과 함께 Phase 0 문서·게이트, 게이트웨이/Kafka/Compose 셋업·리뷰 |
| 김지민 | 관리자 웹 | 관리자 화면. 단 ai 관련 admin 호출이 게이트웨이 경유로 바뀌는 부분 반영 |
| 이지윤 | QA | 계약·통합·회복탄력성 테스트, CI 게이트 갱신, `11_테스트_전략서` 기준 범위 |

> ⚠️ 용량 리스크: MSA는 ai 추출뿐 아니라 **게이트웨이·Kafka·관측성·CI·K8s 같은 횡단(cross-cutting) 작업**이 크다. 강상민·이승욱 2명만으로는 과부하다. 위 표처럼 **Lead가 인프라/거버넌스를 겸임**하거나, 관리자 웹 일정을 ai 추출 이후로 미루는 조정이 필요하다.

---

## 6. 절대 깨지면 안 되는 동작 (전환 중 회귀 감시)

- 00:00 KST QT 공개, 00:05 AI 해설 job 시딩, 04:00 노출/cache 갱신 — 서비스 경계를 넘어도 동일
- 시뮬레이터 상태(`READY`/`MISSING`/`FAILED`/`DISABLED`)와 버튼 활성화 규칙
- F-15 단발성 Q&A 차단·검증·실패 처리, AI 자유 챗봇/SSE **부재** 유지
- 승인본(`verse_explanations`)만 노출, 검증 전 원문·참조자료 미노출
- AI Provider API key는 `external.llm`에서만 사용, 로그 미기록

---

## 7. 리스크 & 완화

| 리스크 | 완화 |
|---|---|
| 거버넌스 누락 → CI가 빌드 차단 | Phase 0에서 게이트·문서 먼저 변경 (코드보다 선행) |
| 2명 팀에 횡단 작업 과부하 | Lead 겸임 / 관리자 웹 일정 조정 (§5) |
| 배치 타이밍(00:05·04:00) 깨짐 | Phase 3을 별도 단계로 분리, 이벤트 + 회귀 테스트로 검증 |
| 분산 트랜잭션·데이터 정합성 | DB 분리를 Phase 4로 미루고 공유 DB로 먼저 안정화 |
| 일정 초과(6/1 재분배본에 없음) | Strangler로 ai 1개만 먼저, 성공 후 반복 |
| 되돌리기 어려움 | 게이트웨이 라우팅만 monolith로 되돌리면 롤백 — 경계를 좁게 유지 |

---

## 8. 완료 정의(DoD) 체크리스트 (1차 = ai 분리)

- [ ] Phase 0 거버넌스·게이트·문서 PR 머지됨
- [ ] ai-service가 독립 배포되고 게이트웨이 경유로 정상 응답
- [ ] monolith의 ai 호출이 REST 어댑터로 동작 (인프로세스 의존 제거)
- [ ] 00:05 시딩 / 04:00 노출이 이벤트 기반으로 동일하게 동작
- [ ] 계약·통합·회복탄력성 테스트 통과, 기존 필수 테스트 그린
- [ ] ArchUnit/경계 테스트가 새 서비스 경계 기준으로 갱신·통과
- [ ] 관련 문서(03 중심) 최신화 완료

---

## 9. 검증 명령 (전환 작업용, 기존 + 추가)

```bash
./gradlew -p qtai-server build
./gradlew -p qtai-server test jacocoTestReport
./gradlew -p qtai-server jacocoTestCoverageVerification
# (신규) ai-service 모듈 분리 후
./gradlew -p qtai-ai-service build test
# 계약/스펙 린트
npx @stoplight/spectral-cli lint apis/*/openapi.yaml --ruleset .spectral.yaml
gitleaks detect --source . --redact --exit-code 1
```

> 비고: 구현 레포는 현재 Windows/Linux git 인덱스 형식 차이로 일부 환경에서 git 명령이 막힐 수 있다(`unknown index entry format`). 본 문서는 그와 무관하게 작성되었으며, 커밋은 정상 동작하는 환경(예: Windows git)에서 진행한다.
