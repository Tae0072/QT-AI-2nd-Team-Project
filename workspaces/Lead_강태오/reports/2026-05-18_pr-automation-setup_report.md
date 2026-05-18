# Report — 2026-05-18 pr-automation-setup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강태오 (@Tae0072) |
| 브랜치 | master (직접 push, W0 한정 예외) |
| PR 링크 | 해당 없음 (master 직접 push) |
| Commit 1/2 | `fc1f653226e858aa718aba0622bd1816bb1b32b1` — CODEOWNERS + claude-pr-review.yml + workspaces 신규 |
| Commit 2/2 | push 직후 GitHub Actions에서 확인 — ci.yml + pull_request_template.md + workspaces 업데이트 |
| 작업 시간 | 2026-05-18 |

## 변경 내용

W0 초기 셋업 단계 PR 자동화 틀 마련. 총 4파일을 v3.1 거버닝 SSoT 기준으로 정합화하고, workspaces 2파일로 작업 기록 정리. **2회의 master 직접 push 단일 commit으로 분할**.

### Commit 1/2 — CODEOWNERS + claude-pr-review.yml + workspaces 신규

#### 1-1. `.github/CODEOWNERS` — v3.1 Modular Monolith 13 도메인 매핑

**기존 잘못된 점:**
- `services/gateway/` ~ `services/ai-service/` 4모듈 경로 (v2.x MSA 잔재)
- `services/bible-service`에 이지윤 + 이승욱 = 이승욱은 note 담당 (오매핑)
- `services/ai-service`에 강상민 + 김태혁 = 김태혁은 simulator 담당 (오매핑)
- 누락된 도메인 매핑 8개 (note/sharing/praise/report/notification/mission/admin/audit)
- workspaces 폴더 매핑 없음

**변경 사항:**
- 13 도메인 패키지 단위 매핑 (`com.qtai.domain.{member, bible, qt, study, note, sharing, report, notification, praise, mission, ai, admin, audit}`)
- 바이블 서버 협업 그룹 = 김지민(@rmfdnjf98) + 이승욱(@LeeSeung-Wook) + 이지윤(@ij447504-source) 3인 공동 — Lead 결정:
  - `bible` / `report` / `notification` / `mission` 4개 도메인 공동
  - `flutter-app/` 전체 공동
  - `apis/` 공동
- `study` = 김태혁(시뮬레이터) + 강상민(해설/용어 협업) + Lead
- `ai` = 강상민 단독 + Lead
- `note` / `sharing` / `praise` = 이승욱 + Lead
- 강태오(@Tae0072)는 모든 도메인에 추가 owner — "모든 파트 도움 권한" (Lead 결정)
- 개인 워크스페이스 6명 매핑 추가
- `data/bible-sources/` → 이지윤 단독
- `infra/`, `.github/`, `docs/`, DECISIONS.md, CLAUDE.md, AGENTS.md, README.md → Lead 단독

#### 1-2. `.github/workflows/claude-pr-review.yml` — v3.1 정합 리뷰 기준 9개

**기존 잘못된 점:**
- 리뷰 기준 6번 "MSA 주의사항: Kafka 이벤트 페어" → v3.1은 Modular Monolith + Kafka 금지
- 리뷰 기준 7번 "AI 코칭 턴 처리" → v3.1은 AI 단발성 (메모리 13)
- `DIFF_MAX_BYTES` = 8000 (8KB) → 대형 PR에서 컨텍스트 손실
- `ANTHROPIC_MODEL` = `claude-sonnet-4-6` → 코드 리뷰 추론력 한정
- 테스트 커버리지·예외 처리 기준 없음

**변경 사항:**
- **env 변수로 추출** (변경 시 한 곳만 수정):
  - `DIFF_MAX_BYTES: 1000000` (1MB) — 이미지 시뮬레이션 도입 시 늘리거나 별도 워크플로우 분리
  - `ANTHROPIC_MODEL_NAME: claude-opus-4-7` — sonnet-4-6 로 다운그레이드 시 한 줄만 수정
- 리뷰 기준 7개 → **9개로 확장**:
  1. 코드 품질
  2. **버그 가능성 + 예외 처리** (강화: try-catch 광범위 catch 금지, ErrorCode 매핑, 외부 API circuit breaker)
  3. **테스트 커버리지** (필수 — 단위/통합 테스트, 예외 케이스 테스트, 커버리지 KPI)
  4. 보안 (권한 8 role, JWT, 검증 통과 콘텐츠만 노출, `validation_reference_jobs` 보호)
  5. Spring Boot 3.3.x + Jakarta 호환성 (`javax.*` 금지, `jakarta.*` 사용) + 트랜잭션
  6. Modular Monolith 도메인 경계 (13 도메인, ArchUnit)
  7. 금지 기술/데이터/기능 패턴 (SSE, RAG, Kafka, K8s, 개역개정·ESV·NIV, F-11, 찬양 가사·음원 등)
  8. 도메인 로직 정합성 + 핵심 7정책 (시간 정책, F-15, 시뮬레이터 4상태, 묵상 1일 1건, envelope, 페이징)
  9. 문서·표현 정합성 (금지 표현, "해설" 사용, workflow/report 링크 필수)
- 심각도 표시 추가: `[BLOCK]` / `[WARN]` / `[INFO]`
- 자동 머지 동작은 그대로 유지 (Claude APPROVE + 모든 CI success → squash merge → dev)
- 안전장치 유지: 다른 CI 실패 시 머지 안 함, 10분 타임아웃, CODEOWNERS bypass 안 함

#### 1-3. `workspaces/Lead_강태오/` 작업 기록

- `workflows/2026-05-18_pr-automation-setup.md` 신규 생성
- `reports/2026-05-18_pr-automation-setup_report.md` 본 리포트

### Commit 2/2 — ci.yml + pull_request_template.md + workspaces 업데이트

#### 2-1. `.github/workflows/ci.yml` — 단일 qtai-server 빌드 + Requirements Guard v3.1

**기존 잘못된 점:**
- `spring-build` matrix가 4개 service(gateway/bff-aggregator/bible-service/ai-service) 빌드
- `flutter-test`에 `flutter analyze` 누락 (`flutter test`만)
- `decisions-guard`가 v3.1 금지 패턴 10개 누락
- `decisions-guard`의 "Anthropic 흔적 금지" 검사가 너무 광범위 (claude-pr-review.yml 자체 차단 위험)

**변경 사항:**
- `spring-build` → 단일 `qtai-server` 빌드 + jacoco 커버리지
  - `working-directory: qtai-server` 설정
  - gradlew 존재 여부 검사 (scaffold PR 이전 단계 false positive 방지)
- `flutter-test` → `flutter analyze` + `flutter test` 둘 다 실행
  - `working-directory: flutter-app`
  - pubspec.yaml 존재 여부 검사
- `decisions-guard` → **`requirements-guard`로 개명** (메모리 25 6번 게이트와 통일)
- 금지 패턴 검사 5개 → **15개로 확장**:
  1. PostgreSQL (jdbc:postgresql)
  2. ZooKeeper
  3. SSE / 세션형 AI (`/ai/sessions/`, `SseEmitter`, `text/event-stream`, `Flux<ServerSentEvent`)
  4. RAG / Vector DB / Elasticsearch (ChromaDB, Weaviate, VectorStore, EmbeddingStore, langchain4j embedding)
  5. Kafka (`KafkaTemplate`, `spring-kafka`, `@KafkaListener`)
  6. Kubernetes / Helm (k8s/, helm/, Chart.yaml)
  7. 성경 번역본 (개역개정 / ESV / NIV) — `--vmE "금지|forbidden|prohibit|KRV|KJV"` 화이트리스트
  8. 교회 인증 F-11 (`church_auth`, `churchVerification`, `church_cert`)
  9. AI 찬양 추천 / 가사·음원 (`recommend.*song`, `lyrics_text`, `audio_file_uri`)
  10. Anthropic SDK (`com.anthropic`, `ClaudeStreamService`) — `qtai-server/` 한정 (claude-pr-review.yml 면제)
  11. `javax.*` import (메모리 19: jakarta.* 사용)
  12. 금지 표현 ("저작권 문제 없음", "유실률 0% 보장", "내부 API 경로") — docs/ 디렉터리 면제
  13. Flutter UI 사용자 노출 "주석" (`.dart` 한정, 코드 주석은 OK)
  14. 성서 유니온/두란노 본문 텍스트 저장 (`seobi_union_text`, `sunmoon_text`, `duranno_text`)
  15. `.env` 파일 커밋 (gitleaks 보조)
- 모든 검사에 `[ -d ... ]` 디렉터리 가드 추가 (qtai-server/, flutter-app/, data/ 미생성 단계 false positive 방지)

#### 2-2. `.github/pull_request_template.md` — v3.1 정합 체크리스트

**기존 잘못된 점:**
- Kafka envelope (`payload` → `data`) 체크 → v3.1은 Kafka 자체 금지
- AI SSE 경로 (`/turns`) 체크 → v3.1은 SSE 자체 금지
- Kafka 이벤트 발행 체크 → v3.1은 ApplicationEventPublisher
- 13 도메인 경계 체크 누락
- 핵심 7정책 체크 누락
- workflow/report 링크 섹션 누락
- 자동 머지 안내 없음

**변경 사항:**
- 메타 항목 3개 추가: 관련 F-ID, 기준 문서 명시, 남은 리스크/후속 PR
- 코드 체크 6개 섹션으로 재구성 (A~F):
  - A. 거버닝 SSoT 충돌 (2건)
  - B. 보안·시크릿 (3건)
  - C. 금지 기술/기능 — Requirements Guard 자동 차단 (7건)
  - D. 금지 데이터·번역본 (2건)
  - E. Spring Boot 3.3.x + Jakarta + 도메인 경계 (4건)
  - F. 도메인 로직 + 핵심 7정책 (6건)
  - 총 24개 코드 체크
- 테스트 체크 5개 (단위/통합/예외 케이스/커버리지 KPI/docs 면제)
- workflow/report 경로 필수 섹션
- 자동 머지 안내 (Claude APPROVE + CI success → squash merge → dev)

#### 2-3. `workspaces/Lead_강태오/` 진행 상황 업데이트

- `workflows/2026-05-18_pr-automation-setup.md` step 8, 9, 10, 11 ✅로 변경
- `reports/2026-05-18_pr-automation-setup_report.md` Commit 2/2 섹션 추가

## 검증 결과

- master push 후 `ci.yml` 자동 트리거 (push: branches: [master])
- 첫 commit 후 GitHub Actions에서 spring-build / flutter-test / requirements-guard 결과 확인
- 두 번째 commit 후 갱신된 ci.yml로 재검증
- `claude-pr-review.yml`은 `pull_request` 트리거라 이번 master push에는 작동 안 함 (의도됨)
- CODEOWNERS는 다음 PR부터 review 라우팅 작동
- 메모리 3, 25 갱신 완료

## CI / 자동 리뷰 결과

- 이번 master push: 자동 PR 리뷰 미작동 (의도됨, master 직접 push는 PR 절차 우회)
- `ci.yml` 자동 트리거: `spring-build` / `flutter-test` / `requirements-guard` 결과는 GitHub Actions 페이지에서 확인
  - qtai-server/, flutter-app/ 디렉터리 미생성이라 spring-build와 flutter-test는 "skipping" 메시지로 통과
  - requirements-guard는 디렉터리 가드로 false positive 없이 통과 예상

## 남은 리스크

1. **`ci.yml`의 `services/*` matrix 폐지 시점에 누군가 services/ 디렉터리에 코드 넣어둔 게 있으면 빌드 실패** — 사전 검사 완료, services/ 디렉터리 미존재 확인
2. **메모리 23(09_Git_규칙) "dev/master 직접 push 금지"는 그대로 유지** — 이번 master push는 W0 한정 예외. 문서/메모리에 예외 조항 추가 검토 필요
3. **자동 머지 정책 인지 부족 위험** — 팀원들에게 "Claude APPROVE + CI success → 자동 머지" 정책 사전 공지 필요. PR template에 안내 명시했으나 첫 실행 시 별도 공유 필요
4. **`DIFF_MAX_BYTES=1MB` Claude 컨텍스트 한도 근접** — 매우 큰 PR(코드 + 시뮬레이션 이미지 base64)에서 컨텍스트 잘릴 수 있음. 이미지 도입 시 별도 워크플로우 분리
5. **누락 게이트(gitleaks, Spectral lint, docker compose config)** — PR-B로 별도 진행 예정
6. **첫 6 PR 중 5번(docs(data)) 머지 후 Spectral은 `apis/` 디렉터리 없으므로 skip** — PR-B에서 디렉터리 존재 가드 필요

## 다음 작업

### 즉시 (필요 시)
- GitHub Actions에서 master push 후 ci.yml 결과 확인
- 메모리 23(09_Git_규칙)에 W0 예외 조항 추가 검토 (선택)

### W1 이후 (정상 PR 절차)
1. **PR-B**: 누락 게이트 3개 추가 (gitleaks, Spectral lint, docker compose config)
   - 새 브랜치: `chore/infra-quality-gates-add`
   - PR 대상: dev
   - 첫 자동 머지 정책 테스트 케이스가 됨
2. **첫 6 PR 순서대로 진행** (메모리 24):
   - (1) `chore(repo): initialize branch, pr rules and workspaces` — 이미 부분 완료
   - (2) `chore(ci): add qt-ai quality gates` — 본 작업 + PR-B로 완성
   - (3) `chore(server): scaffold modular monolith packages` — Lead
   - (4) `test(server): add domain boundary guards` — Lead
   - (5) `docs(data): add bible source review table` — 이지윤
   - (6) `docs(api): add initial api-v1 openapi contract` — Lead/김지민
