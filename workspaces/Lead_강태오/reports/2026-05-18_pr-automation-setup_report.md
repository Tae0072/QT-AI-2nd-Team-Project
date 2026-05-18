# Report — 2026-05-18 pr-automation-setup

| 항목 | 내용 |
| --- | --- |
| 담당자 | 강태오 (@Tae0072) |
| 브랜치 | master (직접 push) |
| PR 링크 | 해당 없음 (master 직접 push, 이번 한 번 예외) |
| 커밋 | push 직후 GitHub Actions에서 확인 |
| 작업 시간 | 2026-05-18 |

## 변경 내용

W0 초기 셋업 단계 PR 자동화 틀 마련. 본 commit은 4파일(CODEOWNERS 2 + workflow 2 + workspaces 2 = 총 4파일)을 한 번에 master에 push.

### 1. `.github/CODEOWNERS` — v3.1 Modular Monolith 13 도메인 매핑

**기존 잘못된 점**:
- `services/gateway/` ~ `services/ai-service/` 4모듈 경로 (v2.x MSA 잔재)
- `services/bible-service`에 이지윤 + 이승욱 = 이승욱은 note 담당 (오매핑)
- `services/ai-service`에 강상민 + 김태혁 = 김태혁은 simulator 담당 (오매핑)
- 누락된 도메인 매핑 8개 (note/sharing/praise/report/notification/mission/admin/audit)
- workspaces 폴더 매핑 없음

**변경 사항**:
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

### 2. `.github/workflows/claude-pr-review.yml` — v3.1 정합 리뷰 기준 9개

**기존 잘못된 점**:
- 리뷰 기준 6번 "MSA 주의사항: Kafka 이벤트 페어" → v3.1은 Modular Monolith + Kafka 금지
- 리뷰 기준 7번 "AI 코칭 턴 처리" → v3.1은 AI 단발성 (메모리 13)
- `DIFF_MAX_BYTES` = 8000 (8KB) → 대형 PR에서 컨텍스트 손실
- `ANTHROPIC_MODEL` = `claude-sonnet-4-6` → 코드 리뷰 추론력 한정
- 테스트 커버리지·예외 처리 기준 없음

**변경 사항**:
- **env 변수로 추출** (변경 시 한 곳만 수정):
  - `DIFF_MAX_BYTES: 1000000` (1MB) — 이미지 시뮬레이션 도입 시 늘리거나 별도 워크플로우 분리
  - `ANTHROPIC_MODEL_NAME: claude-opus-4-7` — sonnet-4-6 로 다운그레이드 시 한 줄만 수정
- 리뷰 기준 7개 → **9개로 확장**:
  1. 코드 품질
  2. **버그 가능성 + 예외 처리** (강화: try-catch 광범위 catch 금지, ErrorCode 매핑, 외부 API circuit breaker)
  3. **테스트 커버리지** (필수 — 단위/통합 테스트, 예외 케이스 테스트, 커버리지 KPI)
  4. 보안 (권한 8 role, JWT, 검증 통과 콘텐츠만 노출, validation_reference_jobs 보호)
  5. Spring Boot 3.3.x + Jakarta 호환성 (javax.* 금지, jakarta.* 사용) + 트랜잭션
  6. Modular Monolith 도메인 경계 (13 도메인, ArchUnit)
  7. 금지 기술/데이터/기능 패턴 (SSE, RAG, Kafka, K8s, 개역개정/ESV/NIV, F-11, 찬양 가사·음원 등)
  8. 도메인 로직 정합성 + 핵심 7정책 (시간 정책, F-15, 시뮬레이터 4상태, 묵상 1일 1건, envelope, 페이징)
  9. 문서·표현 정합성 (금지 표현, "해설" 사용, workflow/report 링크 필수)
- 심각도 표시 추가: `[BLOCK]` / `[WARN]` / `[INFO]`
- 자동 머지 동작은 그대로 유지 (Claude APPROVE + 모든 CI success → squash merge → dev)
- 안전장치 유지: 다른 CI 실패 시 머지 안 함, 10분 타임아웃, CODEOWNERS bypass 안 함

### 3. 메모리 갱신 (Claude 측 user memory)

- **메모리 3**: 로컬 경로 `C:\workspace\...` 단일 → `C:\workspace\...` 또는 `E:\workspace\...` 양쪽 명시 (사용자 환경 반영)
- **메모리 25**: PR 자동 게이트 8번 "사람 리뷰 승인=머지 차단" → "Claude APPROVE + CI 1~7 success → 자동 squash merge → dev. 사람 리뷰는 머지 후에도 review/comment 가능. CODEOWNERS는 리뷰 라우팅용(자동 머지 우회)."

### 4. workspaces/Lead_강태오/ 정리

- `workflows/2026-05-18_pr-automation-setup.md` — 작업 워크플로우 신규 생성
- `reports/2026-05-18_pr-automation-setup_report.md` — 본 리포트

## 검증 결과

- master push 후 `ci.yml` 자동 실행 트리거 (push: branches: [master]) — 결과 GitHub Actions 페이지 모니터링
- `claude-pr-review.yml`은 `pull_request` 트리거만이라 이번 master push에는 작동 안 함 — 의도됨
- CODEOWNERS는 다음 PR 발생 시 review 라우팅 작동
- 메모리 3, 25 갱신 완료 확인

## CI / 자동 리뷰 결과

- 이번 master push: 자동 PR 리뷰 미작동 (의도됨, master 직접 push는 PR 절차 우회)
- `ci.yml` 자동 트리거: `spring-build` / `flutter-test` / `decisions-guard` 결과는 push 후 GitHub Actions 페이지에서 확인

## 남은 리스크

1. **`ci.yml`의 `services/*` matrix가 아직 v3.1 정합화 안 됨** — 다음 작업에서 단일 `qtai-server` 빌드로 교체
2. **`pull_request_template.md`가 아직 v3.1 정합화 안 됨** — 다음 작업에서 Kafka/SSE 항목 제거 + 12 코드 + 3 메타 체크 추가
3. **`ci.yml`의 `decisions-guard`가 v3.1 금지 패턴 10개 누락** — 다음 작업에서 `requirements-guard`로 개명 + 패턴 보강
4. **누락 게이트 4개**(gitleaks, Spectral lint, docker compose config, flutter analyze) — PR-B로 별도 진행 예정
5. **메모리 23(09_Git_규칙) "dev/master 직접 push 금지"는 그대로 유지** — 이번 master push는 W0 한정 예외. 문서/메모리에 예외 조항 추가 검토 필요할 수 있음.
6. **자동 머지 정책 인지 부족 위험** — 팀원들에게 "Claude APPROVE + CI success → 자동 머지" 정책 사전 공지 필요

## 다음 작업

### 즉시 (W0 같은 날 이어서)
1. `.github/workflows/ci.yml` v3.1 정합화 미리보기 → 사용자 OK → master 직접 push (같은 예외 적용)
   - `services/*` matrix 폐지 → 단일 `qtai-server` 빌드
   - `decisions-guard` → `requirements-guard` 개명
   - v3.1 금지 패턴 10개 추가 (SSE / RAG / Kafka / K8s / 개역개정·ESV·NIV / F-11 교회 인증 / 찬양 / Anthropic SDK / javax.* / 금지 표현)
2. `.github/pull_request_template.md` v3.1 정합화 미리보기 → 사용자 OK → master 직접 push (같은 예외 적용)
   - Kafka/SSE 항목 제거
   - 12개 코드 체크 + 3개 메타 체크 추가
   - workflow/report 링크 필수 섹션

### W1 이후 (정상 PR 절차)
1. **PR-B**: 누락 게이트 4개 추가 — 새 브랜치 `chore/infra-quality-gates-add` → dev로 PR
2. **첫 6 PR 순서대로 진행** (메모리 24):
   - (1) chore(repo): initialize branch, pr rules and workspaces — 이미 부분 완료
   - (2) chore(ci): add qt-ai quality gates — 본 작업 + PR-B로 이어짐
   - (3) chore(server): scaffold modular monolith packages — Lead
   - (4) test(server): add domain boundary guards — Lead
   - (5) docs(data): add bible source review table — 이지윤
   - (6) docs(api): add initial api-v1 openapi contract — Lead/김지민
