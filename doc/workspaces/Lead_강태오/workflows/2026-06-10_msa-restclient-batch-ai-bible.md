# 2026-06-10 MSA 배치 RestClient ① service-ai → bible 워크플로우

> 작업 브랜치: `feature/msa-restclient-batch` (origin/dev-msa 기준, worktree `D:\workspace\QT-AI-batch-rc`)
> 선행: 서비스 간 시스템 인증(공유 HS256 `SystemTokenProvider`/`SystemTokenValidator` + 필터 폴백)이 PR #440으로 dev-msa(a01a6d4)에 머지됨. 이걸 첫 소비자로 사용한다.
> 분리 원칙: RestClient 세션(QT-AI-restclient, 사용자맥락 호출 sharing→member/note→qt)과 겹치지 않게 **배치/시스템(SYSTEM_BATCH) 호출쌍만** 담당.

## 목표

service-ai의 해설 생성 배치가 성경 본문 조회에 쓰는 `GetBibleVerseUseCaseMock`을 **service-bible를 HTTP로 호출하는 RestClient 어댑터**로 교체한다. 배치는 전달할 사용자 JWT가 없으므로 어댑터가 단명 시스템 토큰을 발급해 Bearer 헤더로 보낸다.

## 호출쌍 분할 결정

상위 지시의 ① "service-ai 배치 → bible/qt/study"는 실제로 **세 개의 독립 호출쌍**(ai→bible, ai→qt, ai→study)이다. 프로젝트의 기존 RestClient PR(#435 note→bible, #437 note→qt, #439 sharing→member)이 1쌍=1PR이었고, 첫 푸시 통과 체크리스트(PR 10파일·500줄 이하)에도 맞으므로 **쌍 단위로 PR을 나눈다**. 본 문서는 첫 쌍 **ai→bible**.

## TODO

- [x] **0. 분석** — service-ai 내부가 주입받는 cross-service UseCase 전수 조사
  - `ExplanationGenerationJobHandler` → `GetBibleVerseUseCase`(bible) + `GetQtPassageContentContextUseCase`(qt)
  - 배치가 실제로 호출하는 bible 메서드는 `getVerses(List<Long>)`(by-ids). 수신 엔드포인트 `GET /api/v1/bible/verses/by-ids`는 PR #435에서 이미 존재
- [x] **1. 어댑터 작성** — `ai/client/bible/GetBibleVerseRestClientAdapter implements GetBibleVerseUseCase`
- [x] **2. Mock 삭제** — `GetBibleVerseUseCaseMock`(어댑터가 대체, CLAUDE.md §4)
- [x] **3. 단위테스트** — `GetBibleVerseRestClientAdapterTest`(MockRestServiceServer)
- [x] **4. 빌드 검증** — 호스트 `:service-ai:build` GREEN
- [x] **5. 문서** — 워크플로/리포트/스터디노트
- [ ] **6. 커밋·푸시·PR** — base=dev-msa, head=feature/msa-restclient-batch
- [ ] **7. 리뷰 대응** — claude-review APPROVE까지

## 설계 결정·근거

- **인증 = 시스템 토큰 발급(사용자 토큰 전달 아님)**: 사용자맥락 어댑터(note→bible)는 `ServiceCallAuthForwarder`로 요청 JWT를 전달하지만, 해설 생성 배치는 요청 컨텍스트가 없다. 따라서 `SystemTokenProvider.issueSystemToken()`(HS256 단명 SYSTEM_BATCH)으로 발급해 `Authorization: Bearer`로 보낸다. service-bible의 `JwtAuthenticationFilter`가 사용자 RS256 실패 시 시스템 토큰으로 폴백 검증(PR #440).
- **`ObjectProvider<SystemTokenProvider>` 주입**: `SystemTokenProvider`는 `security.jwt.system-secret`이 있을 때만 빈으로 등록된다. service-ai는 테스트/부팅에서 시크릿이 없어도 컨텍스트가 떠야 하므로(`ExplanationGenerationJobHandler`가 `GetBibleVerseUseCase`를 항상 주입받음), 어댑터는 `ObjectProvider`로 받아 **빈 생성은 항상 성공**시키고, 토큰 발급 불가 시 호출 시점에 `EXTERNAL_API_FAILURE`로 실패한다. 생성자 2개(운영 `ObjectProvider` / 테스트 `SystemTokenProvider`)라 운영 생성자에 `@Autowired`를 명시한다.
- **수신측 무변경**: bible SecurityConfig는 `/api/v1/bible/**`를 `authenticated()`로 보호한다. SYSTEM_BATCH 토큰도 인증 주체이므로 그대로 통과한다. 엔드포인트(`/verses/by-ids` 등)도 PR #435에 이미 존재 → service-bible 변경 0.
- **오류 처리(§9)**: 광범위 catch 금지. `RestClientException`만 잡고, 404→`BIBLE_VERSE_NOT_FOUND`, 그 외/언랩 실패→`EXTERNAL_API_FAILURE`. 시스템 토큰·시크릿은 로그/예외 메시지에 남기지 않는다(§7·§9).

## 검증 결과

- 호스트 `.\gradlew.bat --no-daemon :service-ai:build` → **BUILD SUCCESSFUL** (부팅 스모크 포함 전체 통과).
- 어댑터 단위테스트 7건: 단건/다건 정상, 시스템토큰 Bearer 헤더 주입, 404→NOT_FOUND, 5xx→EXTERNAL_API_FAILURE, 빈 목록 단락, 토큰 발급기 미설정 실패.

## 배포 정합(후속 의존성, 코드 PR 범위 밖)

PR #440은 시스템 인증 **메커니즘**만 추가했고 `docker-compose.yml`/`k8s`에 `SECURITY_JWT_SYSTEM_SECRET`을 주입하지 않았다. 배치 호출이 실제 동작하려면 호출자(ai)와 수신자(bible 등)에 동일 시크릿 env가 필요하다. 이는 호출쌍 PR마다 쪼개지 말고 **로컬 배포 세션과 정합한 단일 배포 설정 변경**으로 처리한다(본 코드 PR 본문에 의존성 명시).

## 다음

ai→qt(`GetQtPassageContentContextUseCase`), ai→study(`Publish/List/Hide…VerseExplanationUseCase`) 쌍. study는 쓰기(승인본 게시/숨김) 호출이라 service-bible study 측 수신 엔드포인트 신설이 필요할 수 있어 별도 점검.
