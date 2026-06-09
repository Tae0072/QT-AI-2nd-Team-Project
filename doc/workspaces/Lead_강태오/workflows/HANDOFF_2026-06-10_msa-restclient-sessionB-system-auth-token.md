# HANDOFF — 세션 B: 서비스 간 시스템 인증(공유 HMAC SYSTEM_BATCH 토큰)

> 이 문서 하나로 새 세션이 독립 실행 가능하도록 작성. **배치/시스템 호출(ai→bible·qt·study, user→note/praise/report purge 등)의 선행 조건**.
> 이 메커니즘이 끝나야 SYSTEM_BATCH 맥락의 cross-service 호출을 일괄 진행할 수 있다.

## 0. 시작 절차 (반드시 먼저)

1. 메모리 `qtai-msa-phase1`, `qtai-build-git-toolchain`, `qtai-pr-firstpush-checklist` 읽기.
2. 작업 폴더 `D:\workspace\QT-AI-restclient` 연결. **다른 worktree는 건드리지 말 것.**
3. 최신화 + 브랜치: `git fetch origin` → `git checkout -b feature/msa-system-auth-token origin/dev-msa`.
4. 빌드/그래들/깃은 호스트 PowerShell(JDK21), `gradlew --stop` 금지, commit/push 분리.

## 1. 왜 필요한가 (배경)

- 새 MSA 설계(회의록 §5/§81): JWT는 **service-user가 RS256 개인키로 발급**, 나머지 서비스는 **공유 공개키로 검증만**(`lib-common/JwtValidator`, `JwtAuthenticationFilter`).
- 사용자 요청 맥락 호출은 요청 JWT를 전달하면 됨(이미 `ServiceCallAuthForwarder`로 처리, PR #435/#437).
- **그러나 배치/스케줄러(SYSTEM_BATCH) 호출은 전달할 사용자 JWT가 없다.** 예: `service-ai`의 해설 생성 job이 bible 구절 텍스트를 조회(`ExplanationGenerationJobHandler.getBibleVerseUseCase.getVerses`). service-ai는 검증 공개키만 가져 **스스로 토큰을 못 만든다.**
- 팀의 system token 정의(문서 `DevC_강상민/.../2026-06-05_...system-token-server-call.md`)는 `role=SYSTEM_BATCH` JWT지만 발급은 `JwtProvider`(service-user 개인키)에 의존했고, **§124에서 "service_accounts/shared-secret 인증"을 후속 과제로 명시**해 둠.
- 폐기된 아카이브(95ecfb4)는 게이트웨이가 신원 헤더를 주입하는 구조라 **새 설계(게이트웨이 없음)와 맞지 않음** → 인증 모델 재사용 불가(RestClient 배관 코드만 참고).

## 2. 설계 (Lead 승인 방향: 공유 HMAC 단명 토큰)

서비스 간 시스템 호출용으로 **RS256 사용자 토큰과 분리된, 공유 시크릿(HS256) 기반 단명 SYSTEM_BATCH 토큰**을 도입한다.

- **발급기 `lib-common/common/security/SystemTokenProvider`**:
  - HS256, 시크릿 = `security.jwt.system-secret`(env 주입, ≥256bit). `@ConditionalOnProperty(prefix="security.jwt", name="system-secret")`.
  - claims: `sub="0"`(검증 시 Long 파싱 통과 → principal 0L), `role="SYSTEM_BATCH"`, 단명 TTL(예: 120초), `iss` 마커(예: `qtai-system`).
  - `String issue()` 제공. 토큰 값은 로그 금지(§9).
- **검증(핵심 — blast radius 큼)**: 현재 `JwtAuthenticationFilter`는 `JwtValidator`(RS256)만 쓴다. 시스템 토큰도 받아야 한다.
  - 방식: `lib-common/common/security/SystemTokenValidator`(HS256 verify, 같은 `system-secret`) 추가. `JwtAuthenticationFilter`가 **RS256 사용자 검증 먼저 시도 → 실패(JwtException) 시 SystemTokenValidator 시도**. 시스템 토큰이 유효하면 `role=SYSTEM_BATCH` 권한으로 인증 설정(principal 0L), 둘 다 실패면 401.
  - 두 검증기 모두 `@ConditionalOnProperty`로 게이트(키/시크릿 없는 스켈레톤·테스트는 무영향). 필터는 두 검증기를 `ObjectProvider`로 선택 주입(SecurityConfig 패턴과 동일).
  - 토큰 종류 구분은 alg 헤더(RS256 vs HS256)로 1차 분기해도 됨(파싱 2회 회피). 단순/안전을 우선.
- **호출자 측**: 배치 어댑터는 `Authorization: Bearer {systemTokenProvider.issue()}`를 붙인다. 사용자-요청 어댑터의 `ServiceCallAuthForwarder`와 대비되는 시스템용 경로.

## 3. SSoT 선(先) 갱신 — 반드시 코드보다 먼저 (PR #432 교훈)

CI/리뷰가 "근거 문서부터" 요구한다. 코드 전에:
- `CLAUDE.md` §5(API 규칙)/§시스템 주체에 **서비스 간 시스템 인증 = 공유 시크릿 HS256 단명 SYSTEM_BATCH 토큰(RS256 사용자 토큰과 분리)** 명문화.
- `03_아키텍처_정의서.md`(있으면 구현 저장소 사본)에 inter-service 시스템 인증 절 추가.
- 결정 문서 `doc/workspaces/Lead_강태오/workflows/2026-06-10_inter-service-system-auth.md` 작성(근거·대안·보안 고려).

## 4. 파일 계획

- lib-common: `SystemTokenProvider`(신규), `SystemTokenValidator`(신규), `JwtAuthenticationFilter`(수정: 시스템 토큰 폴백), 설정 키 문서.
- lib-common 테스트: 발급 토큰이 HS256로 검증됨·필터가 ROLE_SYSTEM_BATCH 설정·RS256 사용자 토큰 경로 불변·변조/만료 시스템 토큰 401·시스템 시크릿 없는 서비스는 기존과 동일·RS256↔HS256 교차위조 차단.
- (선택, 별 PR 권장) 첫 소비자: `service-ai`의 `client/bible/GetBibleVerseUseCaseMock`→RestClient 어댑터(배치라 `SystemTokenProvider` 사용). PR을 작게 유지하려면 **메커니즘+테스트+SSoT를 PR1**, 소비자는 후속 PR.

## 5. 인수 조건

- [ ] SSoT(CLAUDE.md/아키텍처/결정문서) 먼저 갱신.
- [ ] `SystemTokenProvider.issue()`가 HS256 SYSTEM_BATCH 토큰 발급(단명 TTL, sub="0").
- [ ] `JwtAuthenticationFilter`가 시스템 토큰을 `ROLE_SYSTEM_BATCH`로 인증, 기존 RS256 사용자 토큰 동작 불변.
- [ ] 변조/만료/교차위조 토큰 401. system-secret 미설정 서비스는 기존과 동일.
- [ ] 멀티모듈 전체 빌드 GREEN(공유 필터라 모든 서비스 영향) — `:lib-common:build :service-user:build :service-bible:build :service-note:build :service-ai:build :admin-server:build`.
- [ ] 시크릿 평문 미커밋(env/k8s Secret), 로그 미노출, gitleaks 통과.

## 6. 위험·주의

- **blast radius**: `JwtAuthenticationFilter`는 모든 서비스가 공유 → 회귀 위험. 기존 JWT 테스트(각 서비스 SecurityIntegrationTest)가 깨지지 않는지 전 모듈 빌드로 확인.
- 시크릿 배포(모든 발급·검증 서비스가 동일 `system-secret` 공유) — `.env`/k8s Secret로만, [[qtai-msa-phase1]] 로컬배포(PR #436) 시크릿 주입 방식과 정합.
- 단명 TTL + 호출마다 발급(상태없음) 권장. 캐시는 선택(시계 오차 고려).
- 첫 푸시 통과: 광범위 catch 금지, 금지 토큰(특히 테스트 문자열) 주의, 보류 테스트 사유 PR 본문 명시.

## 7. 이 메커니즘이 풀어주는 후속 작업(소비자)
- `service-ai` → bible·qt·study (해설 생성 배치)
- `service-user` → note/praise/report/sharing **purge**(retention 배치) — 현재 `member/client/*/Purge...Mock` + `MemberRetentionPurgeService`의 deploy guard(기본 false) 활성화와 함께.
- `service-ai` → audit/admin(admin-server) 기록·권한.
